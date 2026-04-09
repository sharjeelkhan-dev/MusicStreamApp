package com.attendance.app.presentation.students
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.Student
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StudentWithPercentage(
    val student: Student,
    val attendancePercentage: Double = 0.0
)

data class StudentsState(
    val selectedClass: ClassModel? = null,
    val students: List<StudentWithPercentage> = emptyList(),
    val isAddFormVisible: Boolean = false,
    val newStudentName: String = "",
    val newStudentRoll: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editingStudent: Student? = null,
    val error: String? = null,
    val successMessage: String? = null
)

sealed class StudentsEvent {
    data object ToggleAddForm : StudentsEvent()
    data class UpdateNewName(val name: String) : StudentsEvent()
    data class UpdateNewRoll(val roll: String) : StudentsEvent()
    data object AddStudent : StudentsEvent()
    data class DeleteStudent(val student: Student) : StudentsEvent()
    data class StartEditStudent(val student: Student) : StudentsEvent()
    data object CancelEdit : StudentsEvent()
    data object SaveEdit : StudentsEvent()
    data object ClearError : StudentsEvent()
    data object ClearSuccess : StudentsEvent()
}

@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val studentRepository: StudentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(StudentsState())
    val state: StateFlow<StudentsState> = _state.asStateFlow()

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        preferencesManager.selectedClassIdFlow
            .onEach { _state.update { it.copy(isLoading = true) } }
            .flatMapLatest { classId ->
                if (classId != -1L) {
                    val classModel = classRepository.getClassById(classId)
                    _state.update { it.copy(selectedClass = classModel) }
                    
                    studentRepository.getStudentsByClass(classId).map { students ->
                        // Perform heavy calculations and sorting on Default dispatcher
                        withContext(Dispatchers.Default) {
                            students.map { student ->
                                async {
                                    val pct = try {
                                        studentRepository.getAttendancePercentage(student.id, classId)
                                    } catch (_: Exception) {
                                        0.0
                                    }
                                    StudentWithPercentage(student, pct)
                                }
                            }.awaitAll()
                            .sortedByDescending { it.student.createdAt }
                        }
                    }
                } else {
                    // Try to auto-select if classes exist
                    viewModelScope.launch {
                        val classes = classRepository.getAllClasses().firstOrNull()
                        if (!classes.isNullOrEmpty()) {
                            preferencesManager.setSelectedClassId(classes.first().id)
                        }
                    }
                    _state.update { it.copy(selectedClass = null, students = emptyList(), isLoading = false) }
                    flowOf(emptyList())
                }
            }
            .flowOn(Dispatchers.Default) // Ensure the flow processing happens off-main
            .onEach { withPct ->
                _state.update { it.copy(students = withPct, isLoading = false) }
            }
            .catch { e ->
                _state.update { it.copy(isLoading = false, error = "Failed to load: ${e.localizedMessage}") }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: StudentsEvent) {
        when (event) {
            is StudentsEvent.ToggleAddForm -> {
                _state.update {
                    it.copy(
                        isAddFormVisible = !it.isAddFormVisible,
                        newStudentName = "",
                        newStudentRoll = "",
                        editingStudent = null,
                        error = null
                    )
                }
            }
            is StudentsEvent.UpdateNewName -> _state.update { it.copy(newStudentName = event.name) }
            is StudentsEvent.UpdateNewRoll -> _state.update { it.copy(newStudentRoll = event.roll) }
            is StudentsEvent.AddStudent -> addStudent()
            is StudentsEvent.DeleteStudent -> deleteStudent(event.student)
            is StudentsEvent.StartEditStudent -> {
                _state.update {
                    it.copy(
                        editingStudent = event.student,
                        newStudentName = event.student.fullName,
                        newStudentRoll = event.student.rollNumber,
                        isAddFormVisible = true,
                        error = null
                    )
                }
            }
            is StudentsEvent.CancelEdit -> {
                _state.update {
                    it.copy(editingStudent = null, isAddFormVisible = false, newStudentName = "", newStudentRoll = "", error = null)
                }
            }
            is StudentsEvent.SaveEdit -> saveEdit()
            is StudentsEvent.ClearError -> _state.update { it.copy(error = null) }
            is StudentsEvent.ClearSuccess -> _state.update { it.copy(successMessage = null) }
        }
    }

    private fun addStudent() {
        val name = _state.value.newStudentName.trim()
        val roll = _state.value.newStudentRoll.trim()
        val classId = _state.value.selectedClass?.id ?: -1L
        
        if (name.isBlank() || roll.isBlank()) {
            _state.update { it.copy(error = "Name and Roll Number are required!") }
            return
        }

        if (classId == -1L) {
            _state.update { it.copy(error = "Please select a class first!") }
            return
        }

        // Check for duplicate roll number in current class
        val isDuplicate = _state.value.students.any { it.student.rollNumber.equals(roll, ignoreCase = true) }
        if (isDuplicate) {
            _state.update { it.copy(error = "Roll number '$roll' already exists!") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true) }
                val student = Student(fullName = name, rollNumber = roll, classId = classId)
                val id = studentRepository.insertStudent(student)
                
                if (id != -1L) {
                    _state.update {
                        it.copy(
                            newStudentName = "", 
                            newStudentRoll = "", 
                            isAddFormVisible = false,
                            isSaving = false,
                            error = null,
                            successMessage = "Student '$name' ($roll) added successfully!"
                        )
                    }
                } else {
                    _state.update { it.copy(error = "Database error: Could not save student.", isSaving = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error: ${e.localizedMessage}", isSaving = false) }
            }
        }
    }

    private fun deleteStudent(student: Student) {
        viewModelScope.launch {
            try {
                studentRepository.deleteStudent(student)
                _state.update { it.copy(successMessage = "Student deleted successfully.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error: ${e.message}") }
            }
        }
    }

    private fun saveEdit() {
        val editing = _state.value.editingStudent ?: return
        val name = _state.value.newStudentName.trim()
        val roll = _state.value.newStudentRoll.trim()
        
        if (name.isBlank() || roll.isBlank()) {
            _state.update { it.copy(error = "All details are required!") }
            return
        }

        // Check duplicate roll if roll number was changed
        if (!editing.rollNumber.equals(roll, ignoreCase = true)) {
            val isDuplicate = _state.value.students.any { 
                it.student.id != editing.id && it.student.rollNumber.equals(roll, ignoreCase = true) 
            }
            if (isDuplicate) {
                _state.update { it.copy(error = "Roll number '$roll' is already taken by another student!") }
                return
            }
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true) }
                studentRepository.updateStudent(
                    editing.copy(fullName = name, rollNumber = roll)
                )
                _state.update {
                    it.copy(
                        editingStudent = null, 
                        isAddFormVisible = false, 
                        newStudentName = "", 
                        newStudentRoll = "", 
                        isSaving = false,
                        successMessage = "Student details updated successfully."
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Update failed: ${e.message}", isSaving = false) }
            }
        }
    }
}
