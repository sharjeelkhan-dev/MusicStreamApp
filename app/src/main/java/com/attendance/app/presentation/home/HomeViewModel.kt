package com.attendance.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.SessionSummary
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val selectedClass: ClassModel? = null,
    val classes: List<ClassModel> = emptyList(),
    val totalStudents: Int = 0,
    val presentToday: Int = 0,
    val absentToday: Int = 0,
    val recentSessions: List<SessionWithStudents> = emptyList(),
    val isLoading: Boolean = true
)

data class SessionWithStudents(
    val summary: SessionSummary,
    val students: List<Pair<String, Boolean>> // fullName to isPresent
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        observeHomeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeHomeData() {
        val classInfoFlow = combine(
            classRepository.getAllClasses(),
            preferencesManager.selectedClassIdFlow
        ) { classes, selectedId ->
            val selected = classes.find { it.id == selectedId } ?: classes.firstOrNull()
            if (selected != null && selectedId == -1L) {
                preferencesManager.setSelectedClassId(selected.id)
            }
            Pair(classes, selected)
        }

        classInfoFlow.flatMapLatest { (classes, selectedClass) ->
            if (selectedClass != null) {
                combine(
                    studentRepository.getStudentsByClass(selectedClass.id),
                    attendanceRepository.getSessionDates(selectedClass.id)
                ) { students, sessionDates ->
                    if (sessionDates.isEmpty()) {
                        flowOf(HomeState(
                            classes = classes,
                            selectedClass = selectedClass,
                            totalStudents = students.size,
                            presentToday = 0,
                            absentToday = 0,
                            recentSessions = emptyList(),
                            isLoading = false
                        ))
                    } else {
                        val sessionFlows = sessionDates.map { date ->
                            combine(
                                attendanceRepository.getSessionSummary(selectedClass.id, date),
                                attendanceRepository.getAttendanceByClassAndDate(selectedClass.id, date)
                            ) { summary, records ->
                                val studentStatuses = students.map { student ->
                                    val record = records.find { it.studentId == student.id }
                                    val isPresent = record?.status != null && record.status != com.attendance.app.domain.model.AttendanceStatus.ABSENT
                                    student.fullName to isPresent
                                }
                                SessionWithStudents(summary, studentStatuses)
                            }
                        }

                        combine(sessionFlows) { it.toList() }.map { sessions ->
                            val today = LocalDate.now().toString()
                            val todaySession = sessions.find { it.summary.date == today }
                            
                            HomeState(
                                classes = classes,
                                selectedClass = selectedClass,
                                totalStudents = students.size,
                                presentToday = todaySession?.summary?.presentCount ?: 0,
                                absentToday = todaySession?.summary?.absentCount ?: 0,
                                recentSessions = sessions.sortedByDescending { it.summary.date },
                                isLoading = false
                            )
                        }
                    }
                }.flatMapLatest { it }
            } else {
                flowOf(HomeState(classes = classes, isLoading = false))
            }
        }
        .onEach { newState -> _state.value = newState }
        .catch { e ->
            _state.update { it.copy(isLoading = false) }
            e.printStackTrace()
        }
        .launchIn(viewModelScope)
    }

    fun selectClass(classModel: ClassModel) {
        viewModelScope.launch {
            preferencesManager.setSelectedClassId(classModel.id)
        }
    }
}
