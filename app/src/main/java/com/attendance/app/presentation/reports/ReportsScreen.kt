package com.attendance.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.domain.model.AttendanceRecord
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.SessionSummary
import com.attendance.app.domain.model.Student
import com.attendance.app.presentation.components.StandardHeader
import com.attendance.app.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportsContent(
        state = state,
        modifier = modifier,
        paddingValues = paddingValues
    )
}

@Composable
private fun ReportsContent(
    state: ReportsState,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val isDarkGlobal = LocalIsDarkMode.current
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Fixed Header
        StandardHeader(
            title = "Attendance Report",
            subtitle = state.selectedClass?.let {
                "${it.name} — ${it.section}"
            } ?: "No Class Selected"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            // Student Overview section
            item {
                Text(
                    text = "STUDENT OVERVIEW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = if (isDarkGlobal) MaterialTheme.colorScheme.primary else PrimaryGreen)
                    }
                }
            } else if (state.studentReports.isNotEmpty()) {
                item {
                    val sortedReports = remember(state.studentReports) {
                        state.studentReports.sortedByDescending { it.attendancePercentage }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            sortedReports.forEach { report ->
                                val initials = report.student.fullName.split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                
                                val percentage = report.attendancePercentage
                                val percentageColor = when {
                                    percentage >= 80 -> PresentGreen
                                    percentage >= 50 -> LateOrange
                                    else -> AbsentRed
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .offset(y = 5.dp)
                                            .clip(CircleShape)
                                            .background(getAvatarColor(report.student.fullName)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = AvatarTextColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = report.student.fullName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "${percentage.toInt()}%",
                                                color = percentageColor,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        LinearProgressIndicator(
                                            progress = { (percentage / 100f).toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = percentageColor,
                                            trackColor = percentageColor.copy(alpha = 0.1f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No students or attendance data yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Session Details section
            item {
                Text(
                    text = "SESSION DETAILS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 12.dp)
                )
            }

            if (state.sessionDetails.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No session data available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(state.sessionDetails) { session ->
                    val studentStatusList = session.students.map { student ->
                        val record = session.records.find { it.studentId == student.id }
                        student.fullName to (record?.status ?: AttendanceStatus.ABSENT)
                    }.sortedBy { it.second == AttendanceStatus.ABSENT }
                    
                    SessionDetailCard(
                        date = session.summary.date,
                        presentCount = session.summary.presentCount,
                        totalCount = session.summary.totalStudents,
                        students = studentStatusList,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionDetailCard(
    date: String,
    presentCount: Int,
    totalCount: Int,
    students: List<Pair<String, AttendanceStatus>>,
    modifier: Modifier = Modifier
) {
    val isDarkGlobal = LocalIsDarkMode.current
    val parsedDate = try {
        LocalDate.parse(date)
    } catch (_: Exception) {
        LocalDate.now()
    }
    val displayDate = parsedDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$presentCount/$totalCount present",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                students.forEach { (name, status) ->
                    val initials = name.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")

                    val isPresent = status != AttendanceStatus.ABSENT

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPresent) getAvatarColor(name)
                                else if (isDarkGlobal) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = if (isPresent) AvatarTextColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportsScreenPreview() {
    val sampleStudents = listOf(
        Student(id = 1, fullName = "Ahmad Khan", rollNumber = "001", classId = 1),
        Student(id = 2, fullName = "Sara Ahmed", rollNumber = "002", classId = 1),
        Student(id = 3, fullName = "Zainab Bibi", rollNumber = "003", classId = 1)
    )

    val sampleReports = listOf(
        StudentReport(sampleStudents[0], 85.0),
        StudentReport(sampleStudents[1], 45.0),
        StudentReport(sampleStudents[2], 92.0)
    )

    val sampleSessionDetails = listOf(
        SessionWithRecords(
            summary = SessionSummary(
                date = LocalDate.now().toString(),
                totalStudents = 3,
                presentCount = 2,
                absentCount = 1
            ),
            records = listOf(
                AttendanceRecord(studentId = 1, classId = 1, date = LocalDate.now().toString(), status = AttendanceStatus.PRESENT),
                AttendanceRecord(studentId = 2, classId = 1, date = LocalDate.now().toString(), status = AttendanceStatus.ABSENT),
                AttendanceRecord(studentId = 3, classId = 1, date = LocalDate.now().toString(), status = AttendanceStatus.PRESENT)
            ),
            students = sampleStudents
        )
    )

    val sampleState = ReportsState(
        selectedClass = ClassModel(name = "Mobile App Development", section = "BSCS-8A"),
        studentReports = sampleReports,
        sessionDetails = sampleSessionDetails,
        isLoading = false
    )

    AttendanceTheme {
        ReportsContent(state = sampleState)
    }
}
