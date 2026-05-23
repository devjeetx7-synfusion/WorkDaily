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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.dailywork.attedance.data.FirestoreRepository

data class Worker(
    val id: String,
    val name: String,
    val workType: String,
    val wage: Double
)

data class AttendanceRecord(
    val id: String,
    val date: String,
    val status: String,
    val type: String?,
    val reason: String?,
    val overtimeHours: Int?,
    val note: String?,
    val advanceAmount: Double?
)

data class CalendarState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,

    // Contractor State
    val selectedDate: String = "", // YYYY-MM-DD
    val workers: List<Worker> = emptyList(),
    val contractorAttendance: List<AttendanceRecord> = emptyList(),

    // Personal State
    val currentMonthDate: Date = Date(), // Determines the month/year displayed
    val personalAttendance: List<AttendanceRecord> = emptyList()
)

class CalendarViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _calendarState = MutableStateFlow(CalendarState())
    val calendarState: StateFlow<CalendarState> = _calendarState

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var personalAttendanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val workerAttendanceListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        _calendarState.value = _calendarState.value.copy(
            selectedDate = sdf.format(Date())
        )

        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _calendarState.value = _calendarState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
    }

    fun refresh() {
        if (_calendarState.value.isRefreshing) return
        _calendarState.value = _calendarState.value.copy(isRefreshing = true)
        setupListeners(_calendarState.value.role)
    }

    private fun setupListeners(role: String) {
        auth.currentUser ?: return

        workersListener?.remove()
        personalAttendanceListener?.remove()
        workerAttendanceListeners.values.forEach { it.remove() }
        workerAttendanceListeners.clear()

        _calendarState.value = _calendarState.value.copy(isLoading = true)

        if (role == "contractor") {
            workersListener = firestoreRepository.getContractorWorkers()
            ?.limit(50)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        _calendarState.value = _calendarState.value.copy(isLoading = false, isRefreshing = false)
                        return@addSnapshotListener
                    }
                    _calendarState.value = _calendarState.value.copy(isRefreshing = false)
                    val workersList = snapshot.documents.mapNotNull { doc ->
                        Worker(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            workType = doc.getString("workType") ?: "Labour",
                            wage = doc.getDouble("wage") ?: 0.0
                        )
                    }
                    _calendarState.value = _calendarState.value.copy(workers = workersList, isRefreshing = false)
                    updateContractorAttendanceListener()
                }
        } else {
            updatePersonalAttendanceListener()
        }
    }

    private fun updateContractorAttendanceListener() {
        val date = _calendarState.value.selectedDate

        workerAttendanceListeners.values.forEach { it.remove() }
        workerAttendanceListeners.clear()

        _calendarState.value = _calendarState.value.copy(contractorAttendance = emptyList())
        val workers = _calendarState.value.workers

        workers.forEach { worker ->
            val listener = firestoreRepository.getContractorAttendance(worker.id)
                ?.document(date) // Assuming attendanceId is the date
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                         if (snapshot.exists()) {
                             val record = AttendanceRecord(
                                id = "${worker.id}_${snapshot.id}",
                                date = snapshot.getString("date") ?: "",
                                status = snapshot.getString("status") ?: "",
                                type = snapshot.getString("type"),
                                reason = snapshot.getString("reason"),
                                overtimeHours = snapshot.getDouble("overtime_hours")?.toInt(),
                                note = snapshot.getString("note"),
                                advanceAmount = snapshot.getDouble("advance_amount")
                            )
                            _calendarState.update { current ->
                                val newList = current.contractorAttendance.filter { it.id != record.id } + record
                                current.copy(contractorAttendance = newList)
                            }
                         } else {
                             // Handle deletion/missing
                             _calendarState.update { current ->
                                val newList = current.contractorAttendance.filter { it.id != "${worker.id}_${snapshot.id}" }
                                current.copy(contractorAttendance = newList)
                             }
                         }
                    }
                }
            if (listener != null) {
                workerAttendanceListeners[worker.id] = listener
            }
        }
        _calendarState.value = _calendarState.value.copy(isLoading = false)
    }

    private fun updatePersonalAttendanceListener() {
        personalAttendanceListener?.remove()

        val currentMonthDate = _calendarState.value.currentMonthDate
        val cal = Calendar.getInstance()
        cal.time = currentMonthDate

        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthPrefix = sdfMonth.format(cal.time)
        val startDate = "$monthPrefix-01"
        val endDate = "$monthPrefix-31"

        personalAttendanceListener = firestoreRepository.getPersonalAttendance()
            ?.whereGreaterThanOrEqualTo("date", startDate)
            ?.whereLessThanOrEqualTo("date", endDate)
            ?.limit(100)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _calendarState.value = _calendarState.value.copy(isLoading = false, isRefreshing = false)
                    return@addSnapshotListener
                }

                _calendarState.value = _calendarState.value.copy(isRefreshing = false)

                val monthlyAttendance = snapshot.documents.map { doc ->
                    AttendanceRecord(
                        id = doc.id,
                        date = doc.getString("date") ?: "",
                        status = doc.getString("status") ?: "",
                        type = doc.getString("type"),
                        reason = doc.getString("reason"),
                        overtimeHours = doc.getDouble("overtime_hours")?.toInt(),
                        note = doc.getString("note"),
                        advanceAmount = doc.getDouble("advance_amount")
                    )
                }

                _calendarState.value = _calendarState.value.copy(
                    personalAttendance = monthlyAttendance,
                    isLoading = false
                )
            }
    }

    // --- Contractor Actions ---

    fun changeContractorDate(offsetDays: Int) {
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(_calendarState.value.selectedDate) ?: Date()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        _calendarState.value = _calendarState.value.copy(
            selectedDate = sdf.format(cal.time),
            isLoading = true
        )
        updateContractorAttendanceListener()
    }

    fun markContractorAttendance(workerId: String, status: String, type: String) {
        viewModelScope.launch {
            val date = _calendarState.value.selectedDate
            val docId = date // Using date as ID for daily attendance

            val worker = _calendarState.value.workers.find { it.id == workerId }
            val wage = worker?.wage ?: 0.0

            val newData: Map<String, Any?> = mapOf(
                "date" to date,
                "status" to status,
                "type" to if (status == "present") type else null,
                "wage" to wage,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Optimistic UI
            val optimisticRecord = AttendanceRecord(
                id = "${workerId}_$docId",
                date = date,
                status = status,
                type = if (status == "present") type else null,
                reason = null,
                overtimeHours = null,
                note = null,
                advanceAmount = null
            )

            _calendarState.update { current ->
                val newList = current.contractorAttendance.filter { it.id != docId } + optimisticRecord
                current.copy(contractorAttendance = newList)
            }

            try {
                firestoreRepository.markWorkerAttendance(workerId, docId, newData)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // --- Personal Actions ---

    fun changePersonalMonth(offsetMonths: Int) {
        val cal = Calendar.getInstance()
        cal.time = _calendarState.value.currentMonthDate
        cal.add(Calendar.MONTH, offsetMonths)
        _calendarState.value = _calendarState.value.copy(
            currentMonthDate = cal.time,
            isLoading = true
        )
        updatePersonalAttendanceListener()
    }

    fun markPersonalAttendance(
        date: String,
        status: String,
        type: String,
        reason: String,
        overtimeHours: Int,
        note: String,
        advanceAmount: Double
    ) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            val docId = date

            if (status == "present" || status == "absent") {
                val data: Map<String, Any?> = mapOf(
                    "date" to date,
                    "status" to status,
                    "type" to if (status == "present") type else null,
                    "reason" to if (status == "absent") reason else null,
                    "overtime_hours" to if (status == "present") overtimeHours else 0,
                    "note" to if (note.isNotEmpty()) note else null,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                try {
                    firestoreRepository.markPersonalAttendance(docId, data)
                } catch (e: Exception) { }
            }

            if (advanceAmount > 0) {
                val advanceData: Map<String, Any?> = mapOf(
                    "date" to date,
                    "status" to "advance",
                    "advance_amount" to advanceAmount,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                try {
                    firestoreRepository.markPersonalAttendance("${date}_advance", advanceData)
                } catch (e: Exception) { }
            } else {
                try {
                    firestoreRepository.deletePersonalAttendance("${date}_advance", date)
                } catch (e: Exception) { }
            }
        }
    }

    fun removePersonalAttendance(date: String) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            try {
                firestoreRepository.deletePersonalAttendance(date, date)
                firestoreRepository.deletePersonalAttendance("${date}_advance", date)
            } catch (e: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        workersListener?.remove()
        personalAttendanceListener?.remove()
        workerAttendanceListeners.values.forEach { it.remove() }
        workerAttendanceListeners.clear()
    }
}
