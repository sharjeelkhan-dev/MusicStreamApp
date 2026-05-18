package com.attendance.app.presentation.reports
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.domain.model.SessionSummary
import com.attendance.app.domain.model.Student
import com.attendance.app.presentation.components.StandardHeader
import com.attendance.app.presentation.components.VerticalScrollbar
import com.attendance.app.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    var showLoading by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            showLoading = true
        } else {
            delay(800)
            showLoading = false
        }
    }

    ReportsContent(
        state = state,
        showLoading = showLoading,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        paddingValues = paddingValues
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsContent(
    state: ReportsState,
    showLoading: Boolean,
    onEvent: (ReportsEvent) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val isDarkGlobal = LocalIsDarkMode.current
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    // Global state for expanding all session cards at once
    var allSessionsExpanded by remember { mutableStateOf(false) }

    // Pagination state (YouTube style)
    var visibleStudentsLimit by remember(state.studentReports) { mutableIntStateOf(6) }
    var visibleSessionsLimit by remember(state.sessionDetails) { mutableIntStateOf(0) }
    
    val studentsPerPage = 6
    val sessionsPerPage = 4

    val sortedReports = remember(state.studentReports) {
        state.studentReports.sortedBy { it.student.id }
    }
    
    val displayedStudents = sortedReports.take(visibleStudentsLimit)
    val displayedSessions = state.sessionDetails.take(visibleSessionsLimit)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StandardHeader(
            title = "Attendance Report",
            subtitle = state.selectedClass?.let { "${it.name} — ${it.section}" } ?: "No Class Selected"
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(ReportsEvent.Refresh) },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = PrimaryGreen,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 16.dp)
            ) {
                // Header item
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
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

                if (showLoading && !state.isRefreshing) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = if (isDarkGlobal) MaterialTheme.colorScheme.primary else PrimaryGreen)
                        }
                    }
                } else {
                    // Students List
                    items(displayedStudents, key = { it.student.id }) { report ->
                        StudentReportCard(
                            report = report,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .animateItem()
                        )
                    }

                    // Student Loading Trigger
                    if (visibleStudentsLimit < state.studentReports.size) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = if (isDarkGlobal) MaterialTheme.colorScheme.primary else PrimaryGreen,
                                    strokeWidth = 2.dp
                                )
                                LaunchedEffect(visibleStudentsLimit) {
                                    delay(900)
                                    visibleStudentsLimit += studentsPerPage
                                }
                            }
                        }
                    } else if (state.studentReports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No students or attendance data yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Sessions Section
                    if (visibleStudentsLimit >= state.studentReports.size && state.sessionDetails.isNotEmpty()) {
                        item {
                            Text(
                                text = "SESSION HISTORY",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 12.dp)
                            )
                            if (visibleSessionsLimit == 0) {
                                LaunchedEffect(Unit) { visibleSessionsLimit = sessionsPerPage }
                            }
                        }

                        item {
                            // The Main "All Sessions" Container Card
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                    .fillMaxWidth()
                                    .animateContentSize(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkGlobal) 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                                    else Color(0xFFF2F4F7)
                                ),
                                border = if (!isDarkGlobal) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0)) else null
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header of the Container Card
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Session History",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${state.sessionDetails.size} sessions recorded",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        // Only this button has the ripple effect
                                        IconButton(
                                            onClick = { allSessionsExpanded = !allSessionsExpanded },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    if (isDarkGlobal) Color.White.copy(alpha = 0.1f)
                                                    else Color.Black.copy(alpha = 0.05f),
                                                    CircleShape
                                                )
                                            ) {
                                            Icon(
                                                imageVector = if (allSessionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (allSessionsExpanded) "Collapse" else "Expand",
                                                tint = if (isDarkGlobal) Color.White else Color.Black,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Nested Sessions (Only visible when expanded)
                                    if (allSessionsExpanded) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            displayedSessions.forEach { session ->
                                                val sortedStudents = session.studentStatuses.sortedBy { it.second == AttendanceStatus.ABSENT }
                                                SessionDetailCard(
                                                    date = session.summary.date,
                                                    presentCount = session.summary.presentCount,
                                                    totalCount = session.summary.totalStudents,
                                                    students = sortedStudents,
                                                    expanded = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (visibleSessionsLimit > 0 && visibleSessionsLimit < state.sessionDetails.size) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = if (isDarkGlobal) MaterialTheme.colorScheme.primary else PrimaryGreen,
                                        strokeWidth = 2.dp
                                    )
                                    LaunchedEffect(visibleSessionsLimit) {
                                        delay(1200)
                                        visibleSessionsLimit += sessionsPerPage
                                    }
                                }
                            }
                        }
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
    modifier: Modifier = Modifier
) {
    val percentage = report.attendancePercentage
    val statusColor = when {
        percentage >= 80 -> PresentGreen
        percentage >= 60 -> LateOrange
        else -> AbsentRed
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors
            (containerColor = MaterialTheme.
        colorScheme.surface),
        elevation = CardDefaults.
        cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth().offset(x = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = report.student.fullName.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(getAvatarColor(report.student.fullName)),
                contentAlignment = Alignment.Center) {
                Text(text = initials, color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = report.student.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.offset(y = (5.5).dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${report.student.rollNumber} " +
                                " •  ${report.presentCount}/" +
                                "${report.totalSessions} " +
                                "classes",
                            style = MaterialTheme.typography
                                .bodySmall,
                            modifier = Modifier.offset(y = (2).dp),
                            color = MaterialTheme.
                            colorScheme.onSurfaceVariant.
                            copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp)
                    }
                    Text(text = "${percentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.offset(x = (-12).dp).offset(y = 5.5.dp),
                        color = statusColor, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (percentage / 100f).toFloat() },
                    modifier = Modifier.fillMaxWidth(0.94f)
                        .height(4.dp)
                        .offset(y = (-4).dp)
                        .clip(CircleShape),
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round
                )
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
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    val isDarkGlobal = LocalIsDarkMode.current
    val parsedDate = try { LocalDate.parse(date) } catch (_: Exception) { LocalDate.now() }
    val displayDate = parsedDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = if (!isDarkGlobal) androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEAECF0)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                
                Text(
                    text = "$presentCount/$totalCount present",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    students.sortedBy { it.second == AttendanceStatus.ABSENT }.forEach { (name, status) ->
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
                                    else if (isDarkGlobal) Color(0xFF424242) else Color(0xFFF2F4F7)
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
}

@Preview(showBackground = true)
@Composable
private fun SessionHistoryPreview() {
    val sampleSessions = listOf(
        SessionWithRecords(
            summary = SessionSummary(date = "2024-05-20", presentCount = 15, totalStudents = 20, absentCount = 5),
            studentStatuses = listOf(
                "Ahmad Khan" to AttendanceStatus.PRESENT,
                "Sara Ahmed" to AttendanceStatus.PRESENT,
                "Zain Malik" to AttendanceStatus.ABSENT,
                "Hiba Ali" to AttendanceStatus.PRESENT
            )
        ),
        SessionWithRecords(
            summary = SessionSummary(date = "2024-05-18", presentCount = 18, totalStudents = 20, absentCount = 2),
            studentStatuses = listOf(
                "Ahmad Khan" to AttendanceStatus.PRESENT,
                "Sara Ahmed" to AttendanceStatus.ABSENT,
                "Zain Malik" to AttendanceStatus.PRESENT
            )
        )
    )

    ReportsState(
        studentReports = emptyList(),
        sessionDetails = sampleSessions,
        isLoading = false
    )

    var expanded by remember { mutableStateOf(true) }

    AttendanceTheme {
        Box(modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.background)) {
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (LocalIsDarkMode.current) 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                    else Color(0xFFF2F4F7)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "Session History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text(text = "${sampleSessions.size} sessions recorded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }

                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.1f)
                                    else Color.Black.copy(alpha = 0.05f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (LocalIsDarkMode.current) Color.White else Color.Black
                            )
                        }
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sampleSessions.forEach { session ->
                                SessionDetailCard(
                                    date = session.summary.date,
                                    presentCount = session.summary.presentCount,
                                    totalCount = session.summary.totalStudents,
                                    students = session.studentStatuses,
                                    expanded = true,
                                    modifier = Modifier.fillMaxWidth()
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
        Student(id = 2, fullName = "Sara Ahmed", rollNumber = "002", classId = 1, createdAt = System.currentTimeMillis())
    )
    val sampleReports = listOf(StudentReport(sampleStudents[0], 85.0, 17, 20))
    val sampleState = ReportsState(studentReports = sampleReports, sessionDetails = emptyList(), isLoading = false)
    AttendanceTheme { ReportsContent(state = sampleState, showLoading = false, onEvent = {}) }
}
