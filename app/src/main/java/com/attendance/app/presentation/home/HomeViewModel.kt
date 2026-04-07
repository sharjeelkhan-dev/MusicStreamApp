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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
                val today = LocalDate.now().toString()
                
                // Combine everything reactively
                combine(
                    studentRepository.getStudentsByClass(selectedClass.id),
                    attendanceRepository.getSessionDates(selectedClass.id)
                ) { students, sessionDates ->
                    
                    val sessionFlows = sessionDates.map { date ->
                        attendanceRepository.getSessionSummary(selectedClass.id, date).map { summary ->
                            if (summary.totalStudents == 0) return@map null
                            
                            val records = attendanceRepository.getAttendanceByClassAndDate(selectedClass.id, date).firstOrNull() ?: emptyList()
                            val studentStatuses = students.map { student ->
                                val record = records.find { it.studentId == student.id }
                                student.fullName to (record?.status == com.attendance.app.domain.model.AttendanceStatus.PRESENT)
                            }
                            SessionWithStudents(summary, studentStatuses)
                        }
                    }

                    if (sessionFlows.isEmpty()) {
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
                        combine(sessionFlows) { it.toList().filterNotNull().sortedByDescending { s -> s.summary.date } }
                            .map { sessions ->
                                val today = LocalDate.now().toString()
                                val todaySession = sessions.find { it.summary.date == today }
                                
                                HomeState(
                                    classes = classes,
                                    selectedClass = selectedClass,
                                    totalStudents = students.size,
                                    presentToday = todaySession?.summary?.presentCount ?: 0,
                                    absentToday = todaySession?.summary?.absentCount ?: 0,
                                    recentSessions = sessions,
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
