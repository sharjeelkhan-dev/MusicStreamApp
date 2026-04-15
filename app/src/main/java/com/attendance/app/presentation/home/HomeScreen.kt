package com.attendance.app.presentation.home
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.R
import com.attendance.app.presentation.components.*
import com.attendance.app.presentation.theme.*
import java.time.LocalTime

@Composable
fun HomeScreen(
    onNavigateToAttendance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val greeting = when (LocalTime.now().hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importAttendance(context, it) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            // Show toast or snackbar
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    HomeContent(
        state = state,
        greeting = greeting,
        onNavigateToAttendance = onNavigateToAttendance,
        onNavigateToReports = onNavigateToReports,
        onNavigateToStudents = onNavigateToStudents,
        onNavigateToSettings = onNavigateToSettings,
        onImportClick = { filePickerLauncher.launch("*/*") },
        modifier = modifier,
        paddingValues = paddingValues
    )
}

@Composable
private fun HomeContent(
    state: HomeState,
    greeting: String,
    onNavigateToAttendance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val isDark = LocalIsDarkMode.current
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header (Locked at fixed position)
        StandardHeader(
            title = "$greeting 👋",
            subtitle = if (state.selectedClass?.section?.isNotEmpty() == true)
                "${state.selectedClass.name} — ${state.selectedClass.section}"
            else state.selectedClass?.name ?: "No Class",
            showDate = true,
            showSettings = true,
            onSettingsClick = onNavigateToSettings
        )

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                // Stats Row
                item {
                    StatsRow(
                        total = state.totalStudents,
                        present = state.presentToday,
                        absent = state.absentToday
                    )
                }
                    // Quick Actions
                    item {
                        QuickActionsSection(
                            onAttendanceClick = onNavigateToAttendance,
                            onReportsClick = onNavigateToReports,
                            onStudentsClick = onNavigateToStudents,
                            onImportClick = onImportClick
                        )
                    }
                    // Recent Sessions Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 25.dp, end = 20.dp, top = 22.dp, bottom = 8.dp), // Reduced bottom padding
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RECENT SESSIONS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                        .copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                )
                                if (state.selectedClass != null) {
                                    Text(
                                        text = if (state.selectedClass.section.isNotEmpty()) 
                                            "${state.selectedClass.name} — ${state.selectedClass.section}" 
                                            else state.selectedClass.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Surface(
                                onClick = onNavigateToReports,
                                shape = RoundedCornerShape(100), // Perfect Pill shape
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp
                            ) {
                                Text(
                                    text = "See All",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
                                )
                            }
                        }
                    }

                    // Summary Card
                    item {
                        SessionsSummaryCard(
                            sessionCount = state.recentSessions.size,
                            recentPercentages = state.recentSessions.map { it.summary.percentage },
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp) // Removed top padding
                        )
                    }

                    // Recent Sessions List
                    if (state.recentSessions.isEmpty() && !state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No attendance sessions yet.\nTake your first attendance!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(state.recentSessions) { session ->
                            val sortedStudents = remember(session.students) {
                                session.students.sortedBy { !it.second }
                            }
                            SessionCard(
                                date = session.summary.date,
                                presentCount = session.summary.presentCount,
                                absentCount = session.summary.absentCount,
                                percentage = session.summary.percentage,
                                studentNames = sortedStudents,
                                modifier = Modifier.padding(horizontal = 20.dp,
                                    vertical = 8.dp)
                            )
                        }
                    }
                }

                VerticalScrollbar(
                    lazyListState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 12.dp)
                )
            }
        }
    }

@Composable
private fun StatsRow(
    total: Int,
    present: Int,
    absent: Int
) {
    val isDark = LocalIsDarkMode.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 25.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            value = total.toString(),
            label = "Total",
            modifier = Modifier.weight(1f),
            valueColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
        )
        StatsCard(
            value = if (present > 0) present.toString() else "—",
            label = "Present",
            modifier = Modifier.weight(1f),
            valueColor = PresentGreen
        )
        StatsCard(
            value = if (absent > 0) absent.toString() else "—",
            label = "Absent",
            modifier = Modifier.weight(1f),
            valueColor = AbsentRed
        )
    }
}

@Composable
private fun QuickActionsSection(
    onAttendanceClick: () -> Unit,
    onReportsClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
                .copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 5.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = R.drawable.hand_line_icon,
                label = "Attendance",
                isHighlighted = true,
                modifier = Modifier.weight(1f),
                onClick = onAttendanceClick
            )
            QuickActionButton(
                icon = R.drawable.reports_icon,
                label = "Reports",
                modifier = Modifier.weight(1f),
                onClick = onReportsClick
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = R.drawable.graduation_cap_icon,
                label = "Students",
                modifier = Modifier.weight(1f),
                onClick = onStudentsClick
            )
            QuickActionButton(
                icon = R.drawable.import_icon,
                label = "Import",
                modifier = Modifier.weight(1f),
                onClick = onImportClick
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: Any, // ImageVector or Int resource
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else MaterialTheme.colorScheme.onSurfaceVariant

    val iconPainter = when (icon) {
        is ImageVector -> rememberVectorPainter(icon)
        is Int -> painterResource(id = icon)
        else -> throw IllegalArgumentException("Unsupported icon type")
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 12.dp else 8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    AttendanceTheme(darkTheme = false) {
        HomeContent(
            state = HomeState(
                selectedClass = com.attendance.app.domain
                    .model.ClassModel(1, "Software Engineering", "6C1"),
                totalStudents = 45,
                presentToday = 38,
                absentToday = 7,
                recentSessions = listOf(
                    SessionWithStudents(
                        com.attendance.app.domain.model.SessionSummary("2024-05-15", 45, 40, 5),
                        listOf("Aisha Khan" to true, "Bilal Ahmed" to true, "Fatima Malik" to false)
                    ),
                    SessionWithStudents(
                        com.attendance.app.domain.model.SessionSummary("2024-05-14", 45, 38, 7),
                        listOf("Hamza Ali" to true, "Ira Naeem" to false, "Junaid Rashid" to true)
                    ),
                    SessionWithStudents(
                        com.attendance.app.domain.model.SessionSummary("2024-05-12", 45, 42, 3),
                        listOf("Aisha Khan" to true, "Junaid Rashid" to true)
                    )
                ),
                isLoading = false
            ),
            onNavigateToAttendance = {},
            onNavigateToReports = {},
            onNavigateToStudents = {},
            onNavigateToSettings = {},
            onImportClick = {},
            greeting = "Good Morning"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecentSessionsPreview() {
    val dummySessions = listOf(
        com.attendance.app.domain.model.SessionSummary("2024-05-15", 45, 40, 5),
        com.attendance.app.domain.model.SessionSummary("2024-05-14", 45, 38, 7),
        com.attendance.app.domain.model.SessionSummary("2024-05-12", 45, 42, 3)
    )

    AttendanceTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 20.dp)
        ) {
            // Recent Sessions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RECENT SESSIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Software Engineering — 6C1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "See all",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Summary Card
            SessionsSummaryCard(
                sessionCount = dummySessions.size,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Recent Sessions List
            dummySessions.forEach { session ->
                SessionCard(
                    date = session.date,
                    presentCount = session.presentCount,
                    absentCount = session.absentCount,
                    percentage = session.percentage,
                    studentNames = listOf("Aisha Khan" to true, "Bilal Ahmed" to true, "Fatima Malik" to false),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}
