package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.dailywork.attedance.data.FirestoreRepository
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.utils.OvertimeCalculator

class WorkerDetailViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(PassbookState(role = "contractor"))
    val state: StateFlow<PassbookState> = _state

    private var workerId: String? = null
    private var workerListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var cachedDocs: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

    init {
        viewModelScope.launch {
            repository.isPremiumFlow.collect { isPremium ->
                _state.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun initialize(id: String) {
        if (workerId == id) return
        workerId = id
        setupListeners()
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
        auth.currentUser ?: return
        val wId = workerId ?: return

        workerListener?.remove()
        attendanceListener?.remove()

        workerListener = firestoreRepository.getContractorWorkers()?.document(wId)
            ?.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val joinedDateLong = getSafeTimestamp(snapshot, "created_at")
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    _state.value = _state.value.copy(
                        name = snapshot.getString("name") ?: "Worker",
                        workType = snapshot.getString("workType") ?: "Labour",
                        dailyWage = snapshot.getDouble("wage") ?: 500.0,
                        joinedDate = sdf.format(Date(joinedDateLong)),
                        phone = snapshot.getString("phone") ?: ""
                    )
                    calculatePassbook(cachedDocs)
                }
            }

        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        attendanceListener = firestoreRepository.getContractorAttendance(wId)
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
        var totalOTAmount = 0.0
        var totalBaseEarnings = 0.0

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
                totalBaseEarnings += dailyBase
                totalOTAmount += dailyOT
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
        val grossEarned = totalBaseEarnings + totalOTAmount
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

    fun markAttendance(
        date: String,
        status: String,
        type: String,
        reason: String,
        overtimeHours: Int,
        note: String,
        advanceAmount: Double
    ) {
        viewModelScope.launch {
            val wId = workerId ?: return@launch
            val docId = date
            val wage = _state.value.dailyWage

            if (status == "present" || status == "absent") {
                val data: Map<String, Any?> = mapOf(
                    "date" to date,
                    "status" to status,
                    "type" to if (status == "present") type else null,
                    "reason" to if (status == "absent") reason else null,
                    "overtime_hours" to if (status == "present") overtimeHours else 0,
                    "note" to if (note.isNotEmpty()) note else null,
                    "wage" to wage,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                try {
                    firestoreRepository.markWorkerAttendance(wId, docId, data)
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
                    firestoreRepository.markWorkerAttendance(wId, "${date}_advance", advanceData)
                } catch (e: Exception) { }
            } else {
                try {
                    firestoreRepository.deleteWorkerAttendance(wId, "${date}_advance", date)
                } catch (e: Exception) { }
            }
        }
    }

    fun removeAttendance(date: String) {
        viewModelScope.launch {
            val wId = workerId ?: return@launch
            try {
                firestoreRepository.deleteWorkerAttendance(wId, date, date)
                firestoreRepository.deleteWorkerAttendance(wId, "${date}_advance", date)
            } catch (e: Exception) { }
        }
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

    override fun onCleared() {
        super.onCleared()
        workerListener?.remove()
        attendanceListener?.remove()
        userListener?.remove()
    }
}
