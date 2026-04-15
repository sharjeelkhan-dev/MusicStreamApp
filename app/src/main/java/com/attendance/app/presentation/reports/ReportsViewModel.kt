package com.attendance.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.model.*
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import javax.inject.Inject

data class StudentReport(
    val student: Student,
    val attendancePercentage: Double,
    val presentCount: Int,
    val totalSessions: Int
)

data class SessionWithRecords(
    val summary: SessionSummary,
    val studentStatuses: List<Pair<String, AttendanceStatus>>
)

data class ReportsState(
    val selectedClass: ClassModel? = null,
    val studentReports: List<StudentReport> = emptyList(),
    val sessionDetails: List<SessionWithRecords> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

sealed class ReportsEvent {
    data object Refresh : ReportsEvent()
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadData()
    }

    fun onEvent(event: ReportsEvent) {
        when (event) {
            ReportsEvent.Refresh -> {
                _state.update { it.copy(isRefreshing = true) }
                refreshTrigger.tryEmit(Unit)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        combine(
            preferencesManager.selectedClassIdFlow,
            refreshTrigger.onStart { emit(Unit) }
        ) { classId, _ -> classId }
            .flatMapLatest { classId ->
                if (classId != -1L) {
                    flow {
                        emit(ReportsState(isLoading = true, selectedClass = _state.value.selectedClass))
                        
                        val classModel = classRepository.getClassById(classId)
                        val students = studentRepository.getStudentsByClass(classId).first()
                        val dates = attendanceRepository.getSessionDates(classId).first()
                        val allRecords = attendanceRepository.getAllAttendanceForClassFlow(classId).first()

                        // 1. Group records by date and then by studentId for O(1) lookup
                        val recordsByDateAndStudent: Map<String, Map<Long, AttendanceRecord>> = allRecords.groupBy { it.date }
                            .mapValues { entry -> entry.value.associateBy { it.studentId } }

                        // 2. Pre-calculate student creation dates
                        val studentWithDates = students.map { student ->
                            val date = try {
                                Instant.ofEpochMilli(student.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                            } catch (_: Exception) { LocalDate.MIN }
                            student to date
                        }

                        // 3. Process Sessions
                        val sessionList = dates.mapNotNull { dateStr ->
                            try {
                                val sessionDate = LocalDate.parse(dateStr)
                                val studentRecordsMap = recordsByDateAndStudent[dateStr] ?: emptyMap()

                                val enrolledStudents = studentWithDates.filter { !sessionDate.isBefore(it.second) }.map { it.first }
                                if (enrolledStudents.isEmpty()) return@mapNotNull null

                                val presentCount = enrolledStudents.count { 
                                    studentRecordsMap[it.id]?.status == AttendanceStatus.PRESENT 
                                }

                                val studentStatuses = enrolledStudents.map { student ->
                                    student.fullName to (studentRecordsMap[student.id]?.status ?: AttendanceStatus.ABSENT)
                                }

                                SessionWithRecords(
                                    summary = SessionSummary(dateStr, enrolledStudents.size, presentCount, enrolledStudents.size - presentCount),
                                    studentStatuses = studentStatuses
                                ) to sessionDate
                            } catch (_: Exception) { null }
                        }.sortedByDescending { it.second }

                        val finalSessions = sessionList.map { it.first }

                        // 4. Process Student Reports
                        val reports = studentWithDates.map { (student, createdDate) ->
                            var total = 0
                            var present = 0
                            sessionList.forEach { (session, sDate) ->
                                if (!sDate.isBefore(createdDate)) {
                                    total++
                                    if (session.studentStatuses.any { it.first == student.fullName && it.second == AttendanceStatus.PRESENT }) {
                                        present++
                                    }
                                }
                            }
                            val pct = if (total > 0) (present.toDouble() / total * 100.0) else 0.0
                            StudentReport(student, pct, present, total)
                        }.sortedByDescending { it.attendancePercentage }

                        emit(ReportsState(
                            selectedClass = classModel,
                            studentReports = reports,
                            sessionDetails = finalSessions,
                            isLoading = false,
                            isRefreshing = false
                        ))
                    }
                } else {
                    flowOf(ReportsState(isLoading = false))
                }
            }
            .flowOn(Dispatchers.Default)
            .onEach { newState ->
                _state.update { it.copy(
                    selectedClass = newState.selectedClass,
                    studentReports = newState.studentReports,
                    sessionDetails = newState.sessionDetails,
                    isLoading = newState.isLoading,
                    isRefreshing = false
                ) }
            }
            .catch { _ -> _state.update { it.copy(isLoading = false, isRefreshing = false) } }
            .launchIn(viewModelScope)
    }
}
