package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.dailywork.attedance.data.FirestoreRepository
import com.dailywork.attedance.utils.OvertimeCalculator

data class DashboardState(
    val role: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    // Contractor Stats
    val totalWorkers: String = "0",
    val todayPresent: String = "0",
    val todayAbsent: String = "0",
    val totalPaidMonth: String = "0",
    val pendingAmount: String = "0",

    // Personal Stats
    val todayEarned: String = "0",
    val monthEarned: String = "0",

    // Personal Attendance State
    val todayStatus: String? = null,
    val overtimeHours: Int = 0,
    val todayNote: String? = null,

    // Premium state
    val isPremium: Boolean = false,

    // Offline Bottom Sheet State
    val showOfflineBottomSheet: Boolean = false,
    val hasShownOfflineSheet: Boolean = false
)

class DashboardViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _dashboardState = MutableStateFlow(DashboardState(isLoading = true))
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val workerAttendanceListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    // Cache locally to recalculate efficiently on either snapshot change
    private var cachedWorkers: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
    private var cachedAttendance: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
    private var cachedDefaultWage: Double = 500.0

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _dashboardState.value = _dashboardState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
        viewModelScope.launch {
            repository.isPremiumFlow.collect { isPremium ->
                _dashboardState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun refresh() {
        if (_dashboardState.value.isRefreshing) return
        _dashboardState.value = _dashboardState.value.copy(isRefreshing = true)
        setupListeners(_dashboardState.value.role)
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val name = snapshot.getString("name") ?: user.displayName ?: "User"
                val photoUrl = user.photoUrl?.toString() ?: ""
                cachedDefaultWage = snapshot.getDouble("daily_wage") ?: 500.0

                _dashboardState.value = _dashboardState.value.copy(
                    name = name,
                    photoUrl = photoUrl,
                    isRefreshing = false
                )

                recalculateStats()
            }

        if (role == "contractor") {
            workersListener = firestoreRepository.getContractorWorkers()
                ?.limit(100)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cachedWorkers = snapshot.documents
                    setupTodayAttendanceListeners()
                    recalculateStats()
                }

            // For general month stats, we'll fetch from summaries
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonthStr = sdfMonth.format(java.util.Date())
            attendanceListener?.remove()
            attendanceListener = firestoreRepository.contractorSummariesCollection()?.document(currentMonthStr)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _dashboardState.update { it.copy(isRefreshing = false) }
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                         val totalPaid = snapshot.getDouble("total_advance") ?: 0.0
                         val totalWages = snapshot.getDouble("total_wages") ?: 0.0
                         val todayPresent = snapshot.getDouble("today.present_count") ?: 0.0
                         val todayAbsent = snapshot.getDouble("today.absent_count") ?: 0.0
                         val pending = totalWages - totalPaid

                         // We can update monthly stats here
                         _dashboardState.update { it.copy(
                             totalPaidMonth = totalPaid.toInt().toString(),
                             todayPresent = todayPresent.toInt().toString(),
                             todayAbsent = todayAbsent.toInt().toString(),
                             pendingAmount = pending.toInt().toString(),
                             isRefreshing = false
                         ) }
                    } else {
                        _dashboardState.update { it.copy(isRefreshing = false) }
                    }
                }
        } else {
            attendanceListener = firestoreRepository.getPersonalAttendance()
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cachedAttendance = snapshot.documents
                    recalculateStats()
                }
        }
    }

    private val workerTodayStatus = mutableMapOf<String, String?>()

    private fun setupTodayAttendanceListeners() {
        workerAttendanceListeners.values.forEach { it.remove() }
        workerAttendanceListeners.clear()
        workerTodayStatus.clear()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        cachedWorkers.forEach { workerDoc ->
            val workerId = workerDoc.id
            val listener = firestoreRepository.getContractorAttendance(workerId)
                ?.document(todayStr)
                ?.addSnapshotListener { snapshot, _ ->
                    val status = snapshot?.getString("status")
                    workerTodayStatus[workerId] = status

                    // Aggregate and update state
                    val presentCount = workerTodayStatus.values.count { it == "present" }
                    val absentCount = workerTodayStatus.values.count { it == "absent" }

                    _dashboardState.update { it.copy(
                        todayPresent = presentCount.toString(),
                        todayAbsent = absentCount.toString()
                    ) }
                }
            if (listener != null) {
                workerAttendanceListeners[workerId] = listener
            }
        }
    }

    private fun recalculateStats() {
        val role = _dashboardState.value.role
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val currentMonthStr = sdfMonth.format(Date())

        if (role == "contractor") {
            val totalWorkers = cachedWorkers.size
            _dashboardState.update { it.copy(
                isLoading = false,
                totalWorkers = totalWorkers.toString()
            ) }
        } else {
            var todayEarned = 0.0
            var monthEarned = 0.0
            var currentTodayStatus: String? = null
            var currentOvertime = 0
            var currentNote: String? = null

            for (doc in cachedAttendance) {
                val date = doc.getString("date") ?: ""
                val status = doc.getString("status") ?: ""
                val type = doc.getString("type") ?: "full"
                val otHours = doc.getDouble("overtime_hours")?.toInt() ?: 0
                val noteStr = doc.getString("note")

                val dailyBase = if (status == "present") {
                    if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage
                } else 0.0
                val dailyOT = if (status == "present") OvertimeCalculator.calculateOvertimeAmount(cachedDefaultWage, otHours, noteStr) else 0.0

                if (date == todayStr) {
                    if (status != "advance") {
                        currentTodayStatus = status
                        currentOvertime = otHours
                        currentNote = OvertimeCalculator.cleanNote(noteStr)
                        todayEarned = dailyBase + dailyOT
                    }
                }

                if (date.startsWith(currentMonthStr)) {
                    monthEarned += (dailyBase + dailyOT)
                }
            }

            _dashboardState.update { it.copy(
                isLoading = false,
                todayEarned = todayEarned.toInt().toString(),
                monthEarned = monthEarned.toInt().toString(),
                todayStatus = currentTodayStatus,
                overtimeHours = currentOvertime,
                todayNote = currentNote
            ) }
        }
    }

    fun markAttendance(type: String, overtimeHours: Int, note: String) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val docId = todayStr

            _dashboardState.value = _dashboardState.value.copy(
                todayStatus = "present",
                overtimeHours = overtimeHours,
                todayNote = note
            )

            val attendanceData = hashMapOf(
                "date" to todayStr,
                "status" to "present",
                "type" to type,
                "overtime_hours" to overtimeHours,
                "note" to note,
                "daily_wage" to cachedDefaultWage,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            try {
                firestoreRepository.markPersonalAttendance(docId, attendanceData)
            } catch (e: Exception) {
            }
        }
    }

    fun markAbsent(reason: String, note: String) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val docId = todayStr

            _dashboardState.value = _dashboardState.value.copy(
                todayStatus = "absent",
                overtimeHours = 0,
                todayNote = note
            )

            val attendanceData = hashMapOf(
                "date" to todayStr,
                "status" to "absent",
                "reason" to reason,
                "note" to note,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            try {
                firestoreRepository.markPersonalAttendance(docId, attendanceData)
            } catch (e: Exception) {
            }
        }
    }

    fun addAdvance(amount: Double, workerId: String? = null) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val isContractor = _dashboardState.value.role == "contractor"
            val docId = "${todayStr}_advance"

            val advanceData = hashMapOf(
                "date" to todayStr,
                "status" to "advance",
                "advance_amount" to amount,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            try {
                if (isContractor && workerId != null) {
                    firestoreRepository.markWorkerAttendance(workerId, docId, advanceData)
                } else {
                    firestoreRepository.markPersonalAttendance(docId, advanceData)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun removeAttendance() {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val docId = todayStr

            _dashboardState.value = _dashboardState.value.copy(
                todayStatus = null,
                overtimeHours = 0,
                todayNote = null
            )

            try {
                firestoreRepository.deletePersonalAttendance(docId, todayStr)
            } catch (e: Exception) {
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()
        workerAttendanceListeners.values.forEach { it.remove() }
    }

    fun onNetworkStateChanged(isConnected: Boolean) {
        if (!isConnected && !_dashboardState.value.hasShownOfflineSheet) {
            _dashboardState.update { it.copy(showOfflineBottomSheet = true, hasShownOfflineSheet = true) }
        } else if (isConnected) {
            _dashboardState.update { it.copy(showOfflineBottomSheet = false) }
        }
    }

    fun dismissOfflineBottomSheet() {
        _dashboardState.update { it.copy(showOfflineBottomSheet = false) }
    }

    fun retryConnection(isConnected: Boolean) {
        if (isConnected) {
            _dashboardState.update { it.copy(showOfflineBottomSheet = false) }
        }
    }

}