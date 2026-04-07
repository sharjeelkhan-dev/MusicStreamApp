package com.attendance.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.model.AttendanceRecord
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.SessionSummary
import com.attendance.app.domain.model.Student
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentReport(
    val student: Student,
    val attendancePercentage: Double
)

data class SessionWithRecords(
    val summary: SessionSummary,
    val records: List<AttendanceRecord>,
    val students: List<Student>
)

data class ReportsState(
    val selectedClass: ClassModel? = null,
    val studentReports: List<StudentReport> = emptyList(),
    val sessionDetails: List<SessionWithRecords> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            preferencesManager.selectedClassIdFlow.collect { classId ->
                if (classId != -1L) {
                    val classModel = classRepository.getClassById(classId)
                    _state.update { it.copy(selectedClass = classModel, isLoading = true) }

                    // Use combine to react to both students and session dates
                    combine(
                        studentRepository.getStudentsByClass(classId),
                        attendanceRepository.getSessionDates(classId)
                    ) { students, dates ->
                        val reports = students.map { student ->
                            val pct = studentRepository.getAttendancePercentage(student.id, classId)
                            StudentReport(student = student, attendancePercentage = pct)
                        }

                        val sessions = dates.map { date ->
                            val summary = attendanceRepository.getSessionSummary(classId, date)
                            val records = attendanceRepository.getAttendanceByClassAndDate(classId, date).first()
                            SessionWithRecords(summary, records, students)
                        }

                        reports to sessions
                    }.collect { (reports, sessions) ->
                        _state.update {
                            it.copy(
                                studentReports = reports,
                                sessionDetails = sessions,
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, selectedClass = null, studentReports = emptyList(), sessionDetails = emptyList()) }
                }
            }
        }
    }
}
