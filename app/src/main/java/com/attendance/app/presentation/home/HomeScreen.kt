package com.attendance.app.presentation.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.R
import com.attendance.app.presentation.components.SessionCard
import com.attendance.app.presentation.components.StatsCard
import com.attendance.app.presentation.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.EditNote

@Composable
fun HomeScreen(
    onNavigateToAttendance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToClasses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onNavigateToAttendance = onNavigateToAttendance,
        onNavigateToReports = onNavigateToReports,
        onNavigateToStudents = onNavigateToStudents,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
    )
}

@Composable
private fun HomeContent(
    state: HomeState,
    onNavigateToAttendance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().
    background(MaterialTheme.colorScheme.
    background)) {

        // Fixed Header
        HomeHeader(
            className = state.selectedClass?.name ?: "No Class",
            section = state.selectedClass?.section ?: "",
            onSettingsClick = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
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
                    onStudentsClick = onNavigateToStudents
                )
            }

            // Recent Sessions Header
            item {
                Text(
                    text = "RECENT SESSIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
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
                    SessionCard(
                        date = session.date,
                        presentCount = session.presentCount,
                        totalCount = session.totalStudents,
                        percentage = session.percentage,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    className: String,
    section: String,
    onSettingsClick: () -> Unit
) {
    val today = LocalDate.now()
    val dateFormatted = today.format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
    )
    val greeting = when (LocalTime.now().hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(PrimaryGreenDark)
            .padding(top = 16.dp, bottom = 20.dp)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.setting_icon),
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$greeting 👋",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (section.isNotEmpty()) "$className — $section" else className,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun StatsRow(
    total: Int,
    present: Int,
    absent: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            value = total.toString(),
            label = "Total",
            modifier = Modifier.weight(1f),
            valueColor = PrimaryGreen
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
    onStudentsClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.EditNote,
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
            QuickActionButton(
                icon = R.drawable.graduation_cap_icon,
                label = "Students",
                modifier = Modifier.weight(1f),
                onClick = onStudentsClick
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
    val bgColor = if (isHighlighted)
        PrimaryGreen else MaterialTheme.
    colorScheme.surfaceVariant
    val contentColor = if (isHighlighted)
        Color.White else MaterialTheme.
    colorScheme.onSurfaceVariant

    val iconPainter = when (icon) {
        is ImageVector -> rememberVectorPainter(icon)
        is Int -> painterResource(id = icon)
        else -> throw IllegalArgumentException("Unsupported icon type")
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if
                (isHighlighted) 4.dp else 2.dp),
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
    AttendanceTheme {
        HomeContent(
            state = HomeState(
                selectedClass = com.attendance.app.domain
                    .model.ClassModel(1, "Software Engineering", "6C1"),
                totalStudents = 45,
                presentToday = 38,
                absentToday = 7,
                recentSessions = listOf(
                    com.attendance.app.domain.model.SessionSummary("2024-05-15", 45, 40, 5),
                    com.attendance.app.domain.model.SessionSummary("2024-05-14", 45, 38, 7),
                    com.attendance.app.domain.model.SessionSummary("2024-05-12", 45, 42, 3)
                ),
                isLoading = false
            ),
            onNavigateToAttendance = {},
            onNavigateToReports = {},
            onNavigateToStudents = {},
            onNavigateToSettings = {}
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

    AttendanceTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "RECENT SESSIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
            )

            dummySessions.forEach { session ->
                SessionCard(
                    date = session.date,
                    presentCount = session.presentCount,
                    totalCount = session.totalStudents,
                    percentage = session.percentage,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
