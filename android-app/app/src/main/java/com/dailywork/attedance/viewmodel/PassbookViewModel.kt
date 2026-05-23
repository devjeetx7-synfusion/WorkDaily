package com.dailywork.attedance.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.dailywork.attedance.data.FirestoreRepository
import com.dailywork.attedance.utils.OvertimeCalculator

data class PassbookLog(
    val date: String,
    val status: String,
    val type: String?,
    val note: String?,
    val advanceAmount: Double?,
    val overtimeHours: Int = 0,
    val overtimeAmount: Double = 0.0,
    val baseEarnings: Double = 0.0
)

data class PassbookState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val name: String = "",
    val workType: String = "Labour",
    val dailyWage: Double = 500.0,
    val joinedDate: String = "",

    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val totalDailyWorks: Double = 0.0,
    val passedDays: Int = 0,
    val attendanceRate: Int = 0,

    val grossEarned: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val finalBalance: Double = 0.0,
    val phone: String = "",

    val logs: List<PassbookLog> = emptyList(),
    val isPremium: Boolean = false
)

class PassbookViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(PassbookState())
    val state: StateFlow<PassbookState> = _state

    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var cachedDocs: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _state.value = _state.value.copy(role = role)
                    setupListeners()
                }
            }
        }
        viewModelScope.launch {
            repository.isPremiumFlow.collect { isPremium ->
                _state.value = _state.value.copy(isPremium = isPremium)
            }
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.value = _state.value.copy(isRefreshing = true)
        setupListeners()
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _state.value = _state.value.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        )
        setupListeners()
    }

    private fun setupListeners() {
        val user = auth.currentUser ?: return

        userListener?.remove()
        attendanceListener?.remove()

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    // Fallback to name if user doc is missing
                    _state.value = _state.value.copy(name = user.displayName ?: "User", isLoading = false, isRefreshing = false)
                    return@addSnapshotListener
                }

                val joinedDateLong = getSafeTimestamp(snapshot, "created_at")
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                _state.value = _state.value.copy(
                    name = snapshot.getString("name") ?: user.displayName ?: "User",
                    workType = snapshot.getString("workType") ?: "Labour",
                    dailyWage = snapshot.getDouble("daily_wage") ?: 500.0,
                    joinedDate = sdf.format(Date(joinedDateLong)),
                    phone = snapshot.getString("phone") ?: "",
                    isPremium = snapshot.getBoolean("isPremium") ?: false
                )
                calculatePassbook(cachedDocs) // trigger update with current logs if any
            }

        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        attendanceListener = firestoreRepository.getPersonalAttendance()
            ?.whereGreaterThanOrEqualTo("date", "$yearMonth-01")
            ?.whereLessThanOrEqualTo("date", "$yearMonth-31")
            ?.limit(100)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                    return@addSnapshotListener
                }
                cachedDocs = snapshot.documents
                calculatePassbook(snapshot.documents)
            }
    }

    private fun calculatePassbook(docs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate

        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        val passedDays = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var present = 0
        var absent = 0
        var half = 0
        var totalAdv = 0.0
        var totalEarnings = 0.0

        val logMap = mutableMapOf<String, PassbookLog>()
        val wage = _state.value.dailyWage

        docs.forEach { doc ->
            val date = doc.getString("date") ?: return@forEach
            val status = doc.getString("status") ?: ""
            val type = doc.getString("type") ?: "full"
            val adv = doc.getDouble("advance_amount") ?: 0.0
            val note = doc.getString("note")
            val otHours = doc.getDouble("overtime_hours")?.toInt() ?: 0

            var dailyBase = 0.0
            var dailyOT = 0.0

            if (status == "present") {
                present++
                dailyBase = if (type == "half") {
                    half++
                    wage / 2
                } else {
                    wage
                }
                dailyOT = OvertimeCalculator.calculateOvertimeAmount(wage, otHours, note)
                totalEarnings += (dailyBase + dailyOT)
            } else if (status == "absent") {
                absent++
            }
            if (adv > 0) totalAdv += adv

            val existingLog = logMap[date]
            val cleanedNote = OvertimeCalculator.cleanNote(note) ?: existingLog?.note?.takeIf { note == null }

            val mergedOtHours = maxOf(otHours, existingLog?.overtimeHours ?: 0)
            val mergedOtAmount = dailyOT + (existingLog?.overtimeAmount ?: 0.0)
            val mergedBaseEarnings = dailyBase + (existingLog?.baseEarnings ?: 0.0)

            logMap[date] = PassbookLog(
                date = date,
                status = if (status != "advance") status else existingLog?.status ?: "advance",
                type = if (status != "advance") type else existingLog?.type,
                note = cleanedNote,
                advanceAmount = (existingLog?.advanceAmount ?: 0.0) + adv,
                overtimeHours = mergedOtHours,
                overtimeAmount = mergedOtAmount,
                baseEarnings = mergedBaseEarnings
            )
        }

        val effectiveDays = present - (half * 0.5)
        val grossEarned = totalEarnings
        val finalBalance = grossEarned - totalAdv

        val attRate = if (passedDays > 0) ((present.toDouble() / passedDays) * 100).toInt().coerceIn(0, 100) else 0

        val sortedLogs = logMap.values.toList().sortedByDescending { it.date }

        _state.value = _state.value.copy(
            isLoading = false,
            presentDays = present,
            absentDays = absent,
            halfDays = half,
            totalDailyWorks = effectiveDays,
            passedDays = passedDays,
            attendanceRate = attRate,
            grossEarned = grossEarned,
            totalAdvance = totalAdv,
            finalBalance = finalBalance,
            logs = sortedLogs,
            isRefreshing = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        attendanceListener?.remove()
    }

    private fun getSafeTimestamp(snapshot: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
        return try {
            val rawValue = snapshot.get(field)
            when (rawValue) {
                is Timestamp -> rawValue.toDate().time
                is Long -> rawValue
                is String -> rawValue.toLongOrNull() ?: Date().time
                null -> Date().time
                else -> {
                    Log.e("Firestore", "Invalid timestamp format for $field: $rawValue")
                    Date().time
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting timestamp for $field: ${e.message}")
            Date().time
        }
    }
}
