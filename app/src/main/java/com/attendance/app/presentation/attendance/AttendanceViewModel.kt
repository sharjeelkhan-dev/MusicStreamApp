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
import kotlinx.coroutines.delay
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
    val isRefreshing: Boolean = false,
    val attendanceDate: String? = null,
    val error: String? = null
)

sealed class AttendanceEvent {
    data class ToggleStatus(val studentId: Long, val status: AttendanceStatus) : AttendanceEvent()
    object MarkAllPresent : AttendanceEvent()
    object MarkAllAbsent : AttendanceEvent()
    object SaveAttendance : AttendanceEvent()
    data class SearchQueryChanged(val query: String) : AttendanceEvent()
    object Refresh : AttendanceEvent()
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

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadData()
        refreshTrigger.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        combine(
            preferencesManager.selectedClassIdFlow,
            preferencesManager.attendanceDateFlow,
            refreshTrigger.onStart { emit(Unit) }
        ) { classId, attendanceDate, _ -> classId to attendanceDate }
            .filter { it.first != -1L }
            .flatMapLatest { (classId, attendanceDate) ->
                val dateStr = attendanceDate ?: LocalDate.now().toString()
                
                // Combine student list and their attendance records for this date
                combine(
                    studentRepository.getStudentsByClass(classId),
                    attendanceRepository.getAttendanceByClassAndDate(classId, dateStr),
                    flow { emit(classRepository.getClassById(classId)) }
                ) { students, existingRecords, classModel ->
                    val sortedStudents = students.sortedBy { it.createdAt }
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
                        isLoading = false,
                        attendanceDate = dateStr
                    )
                }
            }
            .onEach { newState ->
                _state.update { current ->
                    // Preserve UI-only states like isSaving and searchQuery
                    // But update the data-driven states
                    current.copy(
                        selectedClass = newState.selectedClass,
                        students = if (current.isSaving || current.isSaved) current.students else newState.students,
                        presentCount = if (current.isSaving || current.isSaved) current.presentCount else newState.presentCount,
                        absentCount = if (current.isSaving || current.isSaved) current.absentCount else newState.absentCount,
                        isLoading = newState.isLoading,
                        attendanceDate = newState.attendanceDate,
                        error = newState.error
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
            AttendanceEvent.Refresh -> {
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            delay(800)
            refreshTrigger.emit(Unit)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun saveAttendance() {
        val currentState = _state.value
        val classId = currentState.selectedClass?.id ?: return
        
        viewModelScope.launch {
            try {
                // IMPORTANT: Use the date from the state to ensure we save to the correct day
                val date = currentState.attendanceDate ?: LocalDate.now().toString()
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
                
                // Show "Saved" confirmation
                _state.update { it.copy(isSaving = false, isSaved = true) }
                
                // Wait for 3 seconds
                delay(3000)
                
                // Revert back to "Save"
                _state.update { it.copy(isSaved = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }
}
