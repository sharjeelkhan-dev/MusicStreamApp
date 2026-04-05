package com.attendance.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.attendance.app.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportsContent(
        state = state,
        modifier = modifier
    )
}

@Composable
private fun ReportsContent(
    state: ReportsState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Fixed Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(PrimaryGreenDark)
                .padding(top = 16.dp, bottom = 20.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Attendance Report",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    state.selectedClass?.let {
                        append("${it.name} \u2014 ${it.section}")
                    } ?: append("No Class Selected")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
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

            if (state.studentReports.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            state.studentReports.forEach { report ->
                                ReportStudentRow(
                                    initials = report.student.initials,
                                    name = report.student.fullName,
                                    percentage = report.attendancePercentage
                                )
                            }
                        }
                    }
                }
            } else if (!state.isLoading) {
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
                    NewSessionCard(
                        date = session.summary.date,
                        presentCount = session.summary.presentCount,
                        totalCount = session.summary.totalStudents,
                        sessionDetails = session
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewSessionCard(
    date: String,
    presentCount: Int,
    totalCount: Int,
    sessionDetails: SessionWithRecords
) {
    val parsedDate = try { LocalDate.parse(date) } catch (_: Exception) { LocalDate.now() }
    val displayDate = parsedDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$presentCount/$totalCount present",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sessionDetails.students.forEach { student ->
                    val record = sessionDetails.records.find { it.studentId == student.id }
                    val isPresent = record?.status == AttendanceStatus.PRESENT
                    
                    val studentColor = getAvatarColor(student.fullName)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPresent) studentColor else studentColor.copy(alpha = 0.1f))
                            .border(
                                width = 1.dp,
                                color = if (isPresent) Color.Transparent else studentColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.initials,
                            color = if (isPresent) Color.White else studentColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStudentRow(
    initials: String,
    name: String,
    percentage: Double
) {
    val percentageColor = if (percentage >= 75) PrimaryGreen else AbsentRed

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
                .background(getAvatarColor(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
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
                    text = name,
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
                    .height(6.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = percentageColor,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportsScreenPreview() {
    val dummyStudents = listOf(
        Student(id = 1, fullName = "Aisha Khan", rollNumber = "101", classId = 1),
        Student(id = 2, fullName = "Bilal Ahmed", rollNumber = "102", classId = 1),
        Student(id = 3, fullName = "Fatima Malik", rollNumber = "103", classId = 1)
    )

    val dummyReports = listOf(
        StudentReport(dummyStudents[0], 100.0),
        StudentReport(dummyStudents[1], 50.0),
        StudentReport(dummyStudents[2], 50.0)
    )

    val dummyRecords = listOf(
        AttendanceRecord(studentId = 1, classId = 1, date = "2024-05-10", status = AttendanceStatus.PRESENT),
        AttendanceRecord(studentId = 2, classId = 1, date = "2024-05-10", status = AttendanceStatus.ABSENT),
        AttendanceRecord(studentId = 3, classId = 1, date = "2024-05-10", status = AttendanceStatus.PRESENT)
    )

    val dummySession = SessionWithRecords(
        summary = SessionSummary("2024-05-10", 3, 2, 1),
        records = dummyRecords,
        students = dummyStudents
    )

    AttendanceTheme {
        ReportsContent(
            state = ReportsState(
                selectedClass = ClassModel(1, "Computer Science", "Section A"),
                studentReports = dummyReports,
                sessionDetails = listOf(dummySession),
                isLoading = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportsScreenWithBottomBarPreview() {
    val dummyStudents = listOf(
        Student(id = 1, fullName = "Aisha Khan", rollNumber = "101", classId = 1),
        Student(id = 2, fullName = "Bilal Ahmed", rollNumber = "102", classId = 1),
        Student(id = 3, fullName = "Fatima Malik", rollNumber = "103", classId = 1)
    )

    val dummyReports = listOf(
        StudentReport(dummyStudents[0], 100.0),
        StudentReport(dummyStudents[1], 50.0),
        StudentReport(dummyStudents[2], 50.0)
    )

    val dummyRecords = listOf(
        AttendanceRecord(studentId = 1, classId = 1, date = "2024-05-10", status = AttendanceStatus.PRESENT),
        AttendanceRecord(studentId = 2, classId = 1, date = "2024-05-10", status = AttendanceStatus.ABSENT),
        AttendanceRecord(studentId = 3, classId = 1, date = "2024-05-10", status = AttendanceStatus.PRESENT)
    )

    val dummySession = SessionWithRecords(
        summary = SessionSummary("2024-05-10", 3, 2, 1),
        records = dummyRecords,
        students = dummyStudents
    )

    AttendanceTheme {
        Scaffold(
            bottomBar = {
                com.attendance.app.presentation.components.BottomNavBar(
                    currentRoute = com.attendance.app.presentation.navigation.Screen.Reports.route,
                    onNavigate = {}
                )
            }
        ) { paddingValues ->
            ReportsContent(
                state = ReportsState(
                    selectedClass = ClassModel(1, "Computer Science", "Section A"),
                    studentReports = dummyReports,
                    sessionDetails = listOf(dummySession),
                    isLoading = false
                ),
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
            )
        }
    }
}
