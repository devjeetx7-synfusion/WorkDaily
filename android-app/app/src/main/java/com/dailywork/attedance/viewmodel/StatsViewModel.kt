package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
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
import com.dailywork.attedance.utils.OvertimeCalculator
import kotlinx.coroutines.tasks.await

data class DailyRecord(
    val dateStr: String,
    val earnings: Double,
    val attendance: Double
)

data class WorkerStats(
    val name: String,
    val days: Double,
    val cost: Double
)

data class ContractorStatsData(
    val totalCost: Double = 0.0,
    val totalDailyWorks: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val totalWorkers: Int = 0,
    val topWorkers: List<WorkerStats> = emptyList(),
    val dailyRecords: List<DailyRecord> = emptyList(),
    val allTimeCost: Double = 0.0,
    val allTimeWorks: Double = 0.0,
    val allTimeTopWorkers: List<WorkerStats> = emptyList()
)

data class PersonalStatsData(
    val present: Int = 0,
    val absent: Int = 0,
    val halfDays: Int = 0,
    val overtime: Int = 0,
    val advanceTotal: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val dailyRecords: List<DailyRecord> = emptyList(),
    val allTimeDays: Int = 0,
    val allTimeEarnings: Double = 0.0
)

data class StatsState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val isPremium: Boolean = false,

    // Contractor
    val contractorStats: ContractorStatsData = ContractorStatsData(),

    // Personal
    val personalStats: PersonalStatsData = PersonalStatsData()
)

class StatsViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _statsState = MutableStateFlow(StatsState())
    val statsState: StateFlow<StatsState> = _statsState

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val workerAttendanceListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var summaryListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var cachedDefaultWage: Double = 500.0
    private var cachedWorkers: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _statsState.update { it.copy(role = role) }
                    setupListeners(role)
                }
            }
        }
        viewModelScope.launch {
            repository.isPremiumFlow.collect { isPremium ->
                _statsState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun refresh() {
        if (_statsState.value.isRefreshing) return
        _statsState.value = _statsState.value.copy(isRefreshing = true)
        setupListeners(_statsState.value.role)
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _statsState.value = _statsState.value.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        )
        setupListeners(_statsState.value.role)
    }

    fun setMonth(date: Date) {
        _statsState.value = _statsState.value.copy(
            selectedMonthDate = date,
            isLoading = true
        )
        setupListeners(_statsState.value.role)
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()
        summaryListener?.remove()

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    cachedDefaultWage = snapshot.getDouble("daily_wage") ?: 500.0
                }
            }

        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        if (role == "contractor") {
            workersListener = firestoreRepository.getContractorWorkers()
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        _statsState.update { it.copy(isLoading = false, isRefreshing = false) }
                        return@addSnapshotListener
                    }
                    cachedWorkers = snapshot.documents
                    setupTodayAttendanceListeners()

                    // Fetch all summaries to calculate All-Time stats efficiently
                    attendanceListener?.remove()
                    attendanceListener = firestoreRepository.contractorSummariesCollection()
                        ?.addSnapshotListener { summariesSnapshot, _ ->
                            if (summariesSnapshot != null) {
                                calculateContractorStatsFromSummaries(summariesSnapshot.documents, yearMonth)
                            } else {
                                _statsState.update { it.copy(isRefreshing = false) }
                            }
                        }
                }
        } else {
            attendanceListener = firestoreRepository.getPersonalAttendance()
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        _statsState.update { it.copy(isLoading = false, isRefreshing = false) }
                        return@addSnapshotListener
                    }

                    // Also need personal summaries for all-time
                    summaryListener?.remove()
                    summaryListener = firestoreRepository.personalSummariesCollection()
                        ?.addSnapshotListener { summariesSnapshot, _ ->
                             calculatePersonalStatsWithSummaries(snapshot.documents, summariesSnapshot?.documents ?: emptyList(), yearMonth)
                        }
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

                    val presentCount = workerTodayStatus.values.count { it == "present" }
                    val absentCount = workerTodayStatus.values.count { it == "absent" }

                    _statsState.update { current ->
                        current.copy(
                            contractorStats = current.contractorStats.copy(
                                todayPresent = presentCount,
                                todayAbsent = absentCount
                            )
                        )
                    }
                }
            if (listener != null) {
                workerAttendanceListeners[workerId] = listener
            }
        }
    }

    private fun calculateContractorStatsFromSummaries(summaries: List<com.google.firebase.firestore.DocumentSnapshot>, currentMonth: String) {
        var totalCost = 0.0
        var totalDays = 0.0
        var totalAdvance = 0.0
        var todayPresent = 0
        var todayAbsent = 0

        var allTimeCost = 0.0
        var allTimeWorks = 0.0

        val topWorkersMap = mutableMapOf<String, WorkerStats>()
        val allTimeTopWorkersMap = mutableMapOf<String, WorkerStats>()

        val workersNamesMap = cachedWorkers.associateBy({ it.id }, { it.getString("name") ?: "Unknown" })
        val workersWageMap = cachedWorkers.associateBy({ it.id }, { it.getDouble("wage") ?: 500.0 })

        summaries.forEach { summary ->
            val monthId = summary.id
            val wages = summary.getDouble("total_wages") ?: 0.0
            val present = summary.getDouble("total_present") ?: 0.0
            val advance = summary.getDouble("total_advance") ?: 0.0

            allTimeCost += wages
            allTimeWorks += present

            @Suppress("UNCHECKED_CAST")
            val workersMap = summary.get("workers") as? Map<String, Map<String, Any>>
            workersMap?.forEach { (id, stats) ->
                val d = (stats["days"] as? Number)?.toDouble() ?: 0.0
                val wage = workersWageMap[id] ?: 500.0
                val c = if ((stats["cost"] as? Number)?.toDouble() ?: 0.0 > 0.0) {
                    (stats["cost"] as? Number)?.toDouble() ?: 0.0
                } else {
                    d * wage
                }
                val name = workersNamesMap[id] ?: "Unknown"

                val currentAllTime = allTimeTopWorkersMap[id] ?: WorkerStats(name, 0.0, 0.0)
                allTimeTopWorkersMap[id] = currentAllTime.copy(days = currentAllTime.days + d, cost = currentAllTime.cost + c)
            }

            if (monthId == currentMonth) {
                totalCost = wages
                totalDays = present
                totalAdvance = advance
                todayPresent = summary.getDouble("today.present_count")?.toInt() ?: 0
                todayAbsent = summary.getDouble("today.absent_count")?.toInt() ?: 0

                workersMap?.forEach { (id, stats) ->
                    val d = (stats["days"] as? Number)?.toDouble() ?: 0.0
                    val wage = workersWageMap[id] ?: 500.0
                    val c = if ((stats["cost"] as? Number)?.toDouble() ?: 0.0 > 0.0) {
                        (stats["cost"] as? Number)?.toDouble() ?: 0.0
                    } else {
                        d * wage
                    }
                    val name = workersNamesMap[id] ?: "Unknown"
                    topWorkersMap[id] = WorkerStats(name, d, c)
                }
            }
        }

        _statsState.update { current ->
            current.copy(
                isLoading = false,
                isRefreshing = false,
                contractorStats = current.contractorStats.copy(
                    totalCost = totalCost,
                    totalDailyWorks = totalDays,
                    totalAdvance = totalAdvance,
                    todayPresent = todayPresent,
                    todayAbsent = todayAbsent,
                    totalWorkers = cachedWorkers.size,
                    topWorkers = topWorkersMap.values.sortedByDescending { it.days },
                    allTimeCost = allTimeCost,
                    allTimeWorks = allTimeWorks,
                    allTimeTopWorkers = allTimeTopWorkersMap.values.sortedByDescending { it.days }
                )
            )
        }
    }

    private fun calculatePersonalStatsWithSummaries(
        attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        summaries: List<com.google.firebase.firestore.DocumentSnapshot>,
        currentMonth: String
    ) {
        // Calculate current month from docs (to maintain existing functionality if needed)
        // or just use summaries for most things.

        var allTimeEarnings = 0.0
        var allTimeDays = 0

        var totalEarnings = 0.0
        var present = 0
        var absent = 0
        var halfDays = 0
        var overtime = 0
        var advanceTotal = 0.0

        summaries.forEach { summary ->
            val wages = summary.getDouble("total_wages") ?: 0.0
            val days = summary.getDouble("total_present") ?: 0.0
            allTimeEarnings += wages
            allTimeDays += days.toInt()

            if (summary.id == currentMonth) {
                totalEarnings = wages
                advanceTotal = summary.getDouble("total_advance") ?: 0.0
                // For detailed breakdown (absent, half, overtime), we still need attendanceDocs
            }
        }

        attendanceDocs.forEach { doc ->
            val date = doc.getString("date") ?: ""
            if (date.startsWith(currentMonth)) {
                val status = doc.getString("status") ?: ""
                val type = doc.getString("type") ?: "full"
                val ot = doc.getDouble("overtime_hours")?.toInt() ?: 0

                if (status == "present") {
                    present++
                    if (type == "half") halfDays++
                    overtime += ot
                } else if (status == "absent") {
                    absent++
                }
            }
        }

        _statsState.update { current ->
            current.copy(
                isLoading = false,
                isRefreshing = false,
                personalStats = PersonalStatsData(
                    present = present,
                    absent = absent,
                    halfDays = halfDays,
                    overtime = overtime,
                    advanceTotal = advanceTotal,
                    totalEarnings = totalEarnings,
                    allTimeDays = allTimeDays,
                    allTimeEarnings = allTimeEarnings
                )
            )
        }
    }

    private fun calculateContractorStats(attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>, yearMonth: String) {
        var totalCost = 0.0
        var totalDays = 0.0
        val workerPerfMap = mutableMapOf<String, WorkerStats>()
        val dailyMap = mutableMapOf<String, DailyRecord>()

        var allTimeCost = 0.0
        var allTimeDays = 0.0
        val allTimeWorkerPerfMap = mutableMapOf<String, WorkerStats>()

        val workersMap = cachedWorkers.associateBy({ "worker_${it.id}" }, { it })

        attendanceDocs.forEach { doc ->
            val status = doc.getString("status") ?: ""
            val date = doc.getString("date") ?: ""
            if (status == "present") {
                    val userId = "worker_${doc.reference.parent.parent?.id}"

                val workerDoc = workersMap[userId]

                if (workerDoc != null) {
                    val wage = workerDoc.getDouble("wage") ?: 0.0
                    val type = doc.getString("type") ?: "full"

                    val dayVal = if (type == "half") 0.5 else 1.0
                    val costVal = if (type == "half") wage / 2 else wage

                    // All time stats
                    val allTimeStats = allTimeWorkerPerfMap[userId] ?: WorkerStats(workerDoc.getString("name") ?: "Unknown", 0.0, 0.0)
                    allTimeWorkerPerfMap[userId] = allTimeStats.copy(
                        days = allTimeStats.days + dayVal,
                        cost = allTimeStats.cost + costVal
                    )
                    allTimeDays += dayVal
                    allTimeCost += costVal

                    // Current month stats
                    if (date.startsWith(yearMonth)) {
                        val currentStats = workerPerfMap[userId] ?: WorkerStats(workerDoc.getString("name") ?: "Unknown", 0.0, 0.0)
                        workerPerfMap[userId] = currentStats.copy(
                            days = currentStats.days + dayVal,
                            cost = currentStats.cost + costVal
                        )

                        totalDays += dayVal
                        totalCost += costVal

                        val existingDaily = dailyMap[date] ?: DailyRecord(date, 0.0, 0.0)
                        dailyMap[date] = existingDaily.copy(
                            earnings = existingDaily.earnings + costVal,
                            attendance = existingDaily.attendance + dayVal
                        )
                    }
                }
            }
        }

        val topWorkers = workerPerfMap.values.toList().sortedByDescending { it.days }
        val allTimeTopWorkers = allTimeWorkerPerfMap.values.toList().sortedByDescending { it.days }
        val sortedDailyRecords = dailyMap.values.toList().sortedBy { it.dateStr }

        _statsState.value = _statsState.value.copy(
            isLoading = false,
            isRefreshing = false,
            contractorStats = ContractorStatsData(
                totalCost = totalCost,
                totalDailyWorks = totalDays,
                topWorkers = topWorkers,
                dailyRecords = sortedDailyRecords,
                allTimeCost = allTimeCost,
                allTimeWorks = allTimeDays,
                allTimeTopWorkers = allTimeTopWorkers
            )
        )
    }

    private fun calculatePersonalStats(attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        var present = 0
        var absent = 0
        var halfDays = 0
        var overtime = 0
        var advanceTotal = 0.0
        var totalEarnings = 0.0
        val dailyMap = mutableMapOf<String, DailyRecord>()

        var allTimeDays = 0
        var allTimeEarnings = 0.0

        attendanceDocs.forEach { doc ->
            val date = doc.getString("date") ?: ""
            val status = doc.getString("status") ?: ""
            val type = doc.getString("type") ?: "full"
            val adv = doc.getDouble("advance_amount") ?: 0.0
            val ot = doc.getDouble("overtime_hours")?.toInt() ?: 0
            val noteStr = doc.getString("note")

            val baseCostVal = if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage
            val otAmount = if (status == "present") OvertimeCalculator.calculateOvertimeAmount(cachedDefaultWage, ot, noteStr) else 0.0
            val totalCostVal = baseCostVal + otAmount

            // All time
            if (status == "present") {
                allTimeDays++
                allTimeEarnings += totalCostVal
            }

            // Current month
            if (date.startsWith(yearMonth)) {
                var dayVal = 0.0
                var dayEarn = 0.0

                if (status == "present") {
                    present++
                    if (type == "half") {
                        halfDays++
                        dayVal = 0.5
                    } else {
                        dayVal = 1.0
                    }
                    overtime += ot
                    totalEarnings += totalCostVal
                    dayEarn = totalCostVal
                } else if (status == "absent") {
                    absent++
                }
                if (adv > 0) {
                    advanceTotal += adv
                }

                if (status == "present" || status == "absent") {
                    val existingDaily = dailyMap[date] ?: DailyRecord(date, 0.0, 0.0)
                    dailyMap[date] = existingDaily.copy(
                        earnings = existingDaily.earnings + dayEarn,
                        attendance = existingDaily.attendance + dayVal
                    )
                }
            }
        }

        val sortedDailyRecords = dailyMap.values.toList().sortedBy { it.dateStr }

        _statsState.value = _statsState.value.copy(
            isLoading = false,
            isRefreshing = false,
            personalStats = PersonalStatsData(
                present = present,
                absent = absent,
                halfDays = halfDays,
                overtime = overtime,
                advanceTotal = advanceTotal,
                totalEarnings = totalEarnings,
                dailyRecords = sortedDailyRecords,
                allTimeDays = allTimeDays,
                allTimeEarnings = allTimeEarnings
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()
        summaryListener?.remove()
        workerAttendanceListeners.values.forEach { it.remove() }
    }
}
