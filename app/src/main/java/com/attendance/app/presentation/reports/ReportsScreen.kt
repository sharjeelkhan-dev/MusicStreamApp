package com.attendance.app.presentation.reports
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.attendance.app.presentation.components.VerticalScrollbar
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
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Fixed Header
        StandardHeader(
            title = "Attendance Report",
            subtitle = state.selectedClass?.let {
                "${it.name} — ${it.section}"
            } ?: "No Class Selected"
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                // Student Overview section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STUDENT OVERVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        
                        if (state.studentReports.isNotEmpty()) {
                            Text(
                                text = "${state.studentReports.size} STUDENTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = if (isDarkGlobal) MaterialTheme.colorScheme.primary else PrimaryGreen)
                        }
                    }
                } else if (state.studentReports.isNotEmpty()) {
                    val sortedReports = state.studentReports.sortedByDescending { it.attendancePercentage }
                    
                    itemsIndexed(sortedReports) { index, report ->
                        StudentReportCard(
                            report = report,
                            rank = index + 1,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
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
                }
                else {
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

            VerticalScrollbar(
                lazyListState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 12.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun StudentReportCard(
    report: StudentReport,
    rank: Int,
    modifier: Modifier = Modifier
) {
    val percentage = report.attendancePercentage
    
    val statusColor = when {
        percentage >= 80 -> PresentGreen
        percentage >= 60 -> LateOrange
        else -> AbsentRed
    }
    
    val statusBg = when {
        percentage >= 80 -> PresentGreenBg
        percentage >= 60 -> LateOrangeBg
        else -> AbsentRedBg
    }
    
    val statusText = when {
        percentage >= 80 -> "Excellent"
        percentage >= 60 -> "Average"
        else -> "At Risk"
    }

    val rankSuffix = when (rank) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Avatar
            val initials = report.student.fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(report.student.fullName)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = report.student.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.offset(y = 10.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = statusBg,
                                modifier = Modifier.offset(y = 10.dp),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${report.student.rollNumber}  •  ${report.presentCount}/${report.totalSessions} classes",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.offset(y = 5.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = "${percentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.offset(x = (-8).dp, y = 10.dp),
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (percentage / 100f).toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.08f),
                        strokeCap = StrokeCap.Round
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.width(28.dp).offset(x = (-12).dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "$rank",
                            modifier = Modifier.offset(y = (-2).dp,),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = rankSuffix,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 9.sp,
                            modifier = Modifier.offset(y = (-6.5).dp)
                        )
                    }
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    modifier = Modifier.offset(x = (10).dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Text(
                    text = "$presentCount/$totalCount present",
                    modifier = Modifier.offset(x = (-10).dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().offset(x = 10.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPresent) getAvatarColor(name)
                                else if (isDarkGlobal) Color(0xFF2C2C2C) else Color(0xFFF2F4F7)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = if (isPresent) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        
                        if (!isPresent) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                drawLine(
                                    color = Color.Red.copy(alpha = 0.5f),
                                    start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.5f),
                                    end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.5f),
                                    strokeWidth = 1.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
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
        Student(id = 1, fullName = "Ahmad Khan", rollNumber = "001", classId = 1, createdAt = System.currentTimeMillis()),
        Student(id = 2, fullName = "Sara Ahmed", rollNumber = "002", classId = 1, createdAt = System.currentTimeMillis()),
        Student(id = 3, fullName = "Zainab Bibi", rollNumber = "003", classId = 1, createdAt = System.currentTimeMillis())
    )

    val sampleReports = listOf(
        StudentReport(sampleStudents[0], 85.0, 17, 20),
        StudentReport(sampleStudents[1], 45.0, 9, 20),
        StudentReport(sampleStudents[2], 92.0, 18, 20)
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
