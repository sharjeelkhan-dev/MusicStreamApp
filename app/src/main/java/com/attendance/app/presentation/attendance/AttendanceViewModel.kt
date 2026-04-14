package com.attendance.app.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.model.AttendanceRecord
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.Student
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StudentAttendanceState(
    val student: Student,
    val status: AttendanceStatus
)

data class AttendanceState(
    val selectedClass: ClassModel? = null,
    val students: List<StudentAttendanceState> = emptyList(),
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class AttendanceEvent {
    data class ToggleStatus(val studentId: Long, val status: AttendanceStatus) : AttendanceEvent()
    object MarkAllPresent : AttendanceEvent()
    object MarkAllAbsent : AttendanceEvent()
    object SaveAttendance : AttendanceEvent()
    data class SearchQueryChanged(val query: String) : AttendanceEvent()
}

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        preferencesManager.selectedClassIdFlow
            .filter { it != -1L }
            .flatMapLatest { classId ->
                val classModelFlow = flow { emit(classRepository.getClassById(classId)) }
                val studentsFlow = studentRepository.getStudentsByClass(classId)
                val today = LocalDate.now().toString()
                val attendanceFlow = attendanceRepository.getAttendanceByClassAndDate(classId, today)

                combine(classModelFlow, studentsFlow, attendanceFlow) { classModel, students, existingRecords ->
                    val sortedStudents = students.sortedBy { it.createdAt } // Sort by enrollment date (oldest first)
                    val studentStates = sortedStudents.map { student ->
                        val existing = existingRecords.find { it.studentId == student.id }
                        StudentAttendanceState(
                            student = student,
                            status = existing?.status ?: AttendanceStatus.PRESENT
                        )
                    }
                    AttendanceState(
                        selectedClass = classModel,
                        students = studentStates,
                        presentCount = studentStates.count { s -> s.status == AttendanceStatus.PRESENT },
                        absentCount = studentStates.count { s -> s.status == AttendanceStatus.ABSENT },
                        isLoading = false
                    )
                }
            }
            .onEach { newState -> 
                _state.update { 
                    it.copy(
                        selectedClass = newState.selectedClass,
                        students = newState.students,
                        presentCount = newState.presentCount,
                        absentCount = newState.absentCount,
                        isLoading = false
                    ) 
                } 
            }
            .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AttendanceEvent) {
        when (event) {
            is AttendanceEvent.ToggleStatus -> {
                _state.update { state ->
                    val updatedStudents = state.students.map {
                        if (it.student.id == event.studentId) it.copy(status = event.status)
                        else it
                    }
                    state.copy(
                        students = updatedStudents,
                        presentCount = updatedStudents.count { it.status == AttendanceStatus.PRESENT },
                        absentCount = updatedStudents.count { it.status == AttendanceStatus.ABSENT },
                        isSaved = false
                    )
                }
            }
            AttendanceEvent.MarkAllPresent -> {
                _state.update { state ->
                    val updatedStudents = state.students.map { it.copy(status = AttendanceStatus.PRESENT) }
                    state.copy(
                        students = updatedStudents,
                        presentCount = updatedStudents.size,
                        absentCount = 0,
                        isSaved = false
                    )
                }
            }
            AttendanceEvent.MarkAllAbsent -> {
                _state.update { state ->
                    val updatedStudents = state.students.map { it.copy(status = AttendanceStatus.ABSENT) }
                    state.copy(
                        students = updatedStudents,
                        presentCount = 0,
                        absentCount = updatedStudents.size,
                        isSaved = false
                    )
                }
            }
            AttendanceEvent.SaveAttendance -> {
                saveAttendance()
            }
            is AttendanceEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
        }
    }

    private fun saveAttendance() {
        val currentState = _state.value
        val classId = currentState.selectedClass?.id ?: return
        val date = LocalDate.now().toString()

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val records = currentState.students.map {
                AttendanceRecord(
                    studentId = it.student.id,
                    classId = classId,
                    date = date,
                    status = it.status
                )
            }
            attendanceRepository.saveAttendance(records)
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
