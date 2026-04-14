package com.attendance.app.presentation.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.model.AttendanceRecord
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.SessionSummary
import com.attendance.app.domain.model.Student
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val selectedClass: ClassModel? = null,
    val classes: List<ClassModel> = emptyList(),
    val totalStudents: Int = 0,
    val presentToday: Int = 0,
    val absentToday: Int = 0,
    val recentSessions: List<SessionWithStudents> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
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
                                val sessionDate = try {
                                    LocalDate.parse(summary.date)
                                } catch (e: Exception) { LocalDate.MAX }

                                val enrolledStudents = students.filter { student ->
                                    val studentCreatedDate = try {
                                        java.time.Instant.ofEpochMilli(student.createdAt)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDate()
                                    } catch (e: Exception) { LocalDate.MIN }
                                    !sessionDate.isBefore(studentCreatedDate)
                                }

                                val studentStatuses = enrolledStudents.map { student ->
                                    val record = records.find { it.studentId == student.id }
                                    val isPresent = record?.status != null && record.status != com.attendance.app.domain.model.AttendanceStatus.ABSENT
                                    student.fullName to isPresent
                                }

                                val presentOnDay = studentStatuses.count { it.second }
                                val updatedSummary = summary.copy(
                                    totalStudents = enrolledStudents.size,
                                    presentCount = presentOnDay,
                                    absentCount = enrolledStudents.size - presentOnDay
                                )
                                SessionWithStudents(updatedSummary, studentStatuses)
                            }
                        }

                        combine(sessionFlows) { it.toList() }.map { sessions ->
                            val today = LocalDate.now().toString()
                            val todaySessions = sessions.filter { it.summary.date == today }
                            val currentStudentsCount = students.size
                            
                            HomeState(
                                classes = classes,
                                selectedClass = selectedClass,
                                totalStudents = currentStudentsCount,
                                presentToday = todaySessions.sumOf { it.summary.presentCount },
                                absentToday = todaySessions.sumOf { it.summary.absentCount },
                                recentSessions = todaySessions,
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

    fun importAttendance(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.update { it.copy(isLoading = true) }
                val inputStream = context.contentResolver.openInputStream(uri)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                
                // Assume Header Row 0
                // Column 0: Class Name, Column 1: Section, Column 2: Date (YYYY-MM-DD), 
                // Column 3: Roll No, Column 4: Student Name, Column 5: Status (P/A)
                
                val recordsToSave = mutableListOf<AttendanceRecord>()
                val classCache = mutableMapOf<String, Long>() // "Name-Section" to ID
                
                // Pre-load existing classes into cache
                classRepository.getAllClasses().first().forEach { 
                    classCache["${it.name}-${it.section}".lowercase()] = it.id 
                }
                
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    val className = row.getCell(0)?.toString()?.trim() ?: ""
                    val section = row.getCell(1)?.toString()?.trim() ?: ""
                    val rawDateStr = row.getCell(2)?.toString()?.trim() ?: ""
                    val rollNo = row.getCell(3)?.toString()?.trim() ?: ""
                    val fullName = row.getCell(4)?.toString()?.trim() ?: ""
                    val statusStr = row.getCell(5)?.toString()?.trim()?.uppercase() ?: "A"
                    
                    if (className.isEmpty() || rollNo.isEmpty() || fullName.isEmpty()) continue

                    // 1. Resolve Class
                    val classKey = "$className-$section".lowercase()
                    val classId = if (classCache.containsKey(classKey)) {
                        classCache[classKey]!!
                    } else {
                        val newId = classRepository.insertClass(ClassModel(name = className, section = section))
                        classCache[classKey] = newId
                        newId
                    }

                    // 2. Resolve/Update Student
                    val currentStudents = studentRepository.getStudentsByClass(classId).first()
                    var student = currentStudents.find { it.rollNumber == rollNo }
                    
                    if (student == null) {
                        val newId = studentRepository.insertStudent(
                            Student(fullName = fullName, rollNumber = rollNo, classId = classId)
                        )
                        student = Student(id = newId, fullName = fullName, rollNumber = rollNo, classId = classId)
                    } else if (student.fullName != fullName) {
                        val updatedStudent = student.copy(fullName = fullName)
                        studentRepository.updateStudent(updatedStudent)
                        student = updatedStudent
                    }

                    // 3. Resolve Date
                    val dateStr = try {
                        // Check if it's a numeric date (Excel internal)
                        if (row.getCell(2)?.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            val date = row.getCell(2).dateCellValue
                            date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                        } else {
                            LocalDate.parse(rawDateStr).toString()
                        }
                    } catch (e: Exception) {
                        LocalDate.now().toString()
                    }

                    // 4. Prepare Attendance Record
                    val status = if (statusStr == "P" || statusStr == "PRESENT") 
                        AttendanceStatus.PRESENT else AttendanceStatus.ABSENT

                    recordsToSave.add(
                        AttendanceRecord(
                            studentId = student.id,
                            classId = classId,
                            date = dateStr,
                            status = status
                        )
                    )
                }

                if (recordsToSave.isNotEmpty()) {
                    attendanceRepository.saveAttendance(recordsToSave)
                    _state.update { it.copy(message = "Imported ${recordsToSave.size} records successfully!") }
                } else {
                    _state.update { it.copy(message = "No valid records found in file.") }
                }
                
                workbook.close()
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(message = "Error: ${e.message}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
