package com.attendance.app.presentation.students
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.presentation.components.StatsCard
import com.attendance.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.StudentDetailScreen(
    studentName: String,
    studentRoll: String,
    initials: String,
    avatarColor: Color,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: StudentDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val currentStudentName = state.student?.fullName ?: studentName
    val currentStudentRoll = state.student?.rollNumber ?: studentRoll
    val currentInitials = state.student?.initials ?: initials

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Header Section - Maintained as requested (PrimaryGreenDark background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryGreenDark)
                    .statusBarsPadding()
                    .height(75.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(end = 5.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-5).dp, y = 0.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }

                    Column(modifier = Modifier.padding(top = 14.dp))
                    {
                        Text(
                            text = "Student Details",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentStudentRoll,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Section
            Spacer(modifier = Modifier.height(40.dp))
            Surface(
                shape = CircleShape,
                color = avatarColor,
                modifier = Modifier
                    .size(140.dp)
                    .sharedElement(
                        rememberSharedContentState(key = "avatar_$studentRoll"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = currentInitials,
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name
            Text(
                text = currentStudentName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "name_$studentRoll"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            )
            
            // Roll Number
            Text(
                text = "Roll Number: $currentStudentRoll",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-13).dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    label = "Present",
                    value = String.format(Locale.getDefault(), "%02d", state.presentCount),
                    valueColor = PresentGreen,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    label = "Absent",
                    value = String.format(Locale.getDefault(), "%02d", state.absentCount),
                    valueColor = AbsentRed,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    label = "Attendance",
                    value = String.format(Locale.getDefault(), "%.0f%%", state.attendancePercentage),
                    valueColor = Color(0xFF00ACC1),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Attendance Statistics Graph
            AttendanceChart(
                studentData = state.weeklyStudentData,
                classAvgData = state.weeklyClassAvgData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val enrollmentDate = remember(state.student?.createdAt) {
                        state.student?.createdAt?.let {
                            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "N/A"
                    }
                    DetailRow(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Enrollment Date",
                        value = enrollmentDate
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    DetailRow(
                        icon = Icons.Rounded.School,
                        label = "Assigned Class",
                        value = state.classModel?.let { "${it.name} (${it.section})" } ?: "N/A"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AttendanceChart(
    studentData: List<Float>,
    classAvgData: List<Float>,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Week 1", "Week 2", "Week 3", "Week 4")
    val percentages = listOf("100%", "75%", "50%", "25%", "0%")
    
    // Fixed dimensions for pixel-perfect alignment
    val rowHeight = 32.dp
    val plottingAreaHeight = rowHeight * 4
    val yLabelWidth = 48.dp

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = AbsentRed, label = "you")
                    LegendItem(color = Color(0xFF4A90E2), label = "class avg")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(plottingAreaHeight + rowHeight)) {
                    // 1. Grid Lines & Y-Labels
                    Column(modifier = Modifier.fillMaxSize()) {
                        percentages.forEach { pct ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(rowHeight)
                            ) {
                                Text(
                                    text = pct,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.width(yLabelWidth).padding(end = 12.dp),
                                    textAlign = TextAlign.End,
                                    fontSize = 10.sp
                                )
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    // 2. Bars
                    Row(
                        modifier = Modifier
                            .padding(start = yLabelWidth, top = rowHeight / 2)
                            .height(plottingAreaHeight)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        repeat(4) { index ->
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                val studentValue = studentData.getOrElse(index) { 0f }
                                val classValue = classAvgData.getOrElse(index) { 0f }

                                Bar(
                                    value = studentValue,
                                    color = AbsentRed,
                                    modifier = Modifier.fillMaxHeight((studentValue / 100f).coerceIn(0.01f, 1f))
                                )
                                Bar(
                                    value = classValue,
                                    color = Color(0xFF4A90E2),
                                    modifier = Modifier.fillMaxHeight((classValue / 100f).coerceIn(0.01f, 1f))
                                )
                            }
                        }
                    }
                }

                // 3. X-Axis Labels
                Row(
                    modifier = Modifier
                        .padding(start = yLabelWidth)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun Bar(value: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(12.dp)
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = PrimaryGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun StudentDetailPreview() {
    AttendanceTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "student_detail_preview") {
                val animatedVisibilityScope = this
                val viewModel = remember { StudentDetailViewModelPreview() }
                Box(modifier = Modifier.fillMaxSize()) {
                    StudentDetailScreen(
                        studentName = "John Doe",
                        studentRoll = "CS-01",
                        initials = "JD",
                        avatarColor = PrimaryGreen,
                        animatedVisibilityScope = animatedVisibilityScope,
                        viewModel = viewModel,
                        onBack = {}
                    )
                }
            }
        }
    }
}

@SuppressLint("VisibleForTests")
class StudentDetailViewModelPreview : StudentDetailViewModel(
    studentRepository = object : com.attendance.app.domain.repository.StudentRepository {
        override fun getStudentsByClass(classId: Long) = error("Not implemented")
        override suspend fun getStudentById(studentId: Long) = com.attendance.app.domain.model.Student(1L, "John Doe", "CS-01", 1L)
        override suspend fun insertStudent(student: com.attendance.app.domain.model.Student) = 0L
        override suspend fun updateStudent(student: com.attendance.app.domain.model.Student) {}
        override suspend fun deleteStudent(student: com.attendance.app.domain.model.Student) {}
        override suspend fun getAttendancePercentage(studentId: Long, classId: Long) = 90.0
        override fun searchStudents(classId: Long, query: String) = error("Not implemented")
    },
    classRepository = object : com.attendance.app.domain.repository.ClassRepository {
        override fun getAllClasses() = error("Not implemented")
        override suspend fun getClassById(classId: Long) = com.attendance.app.domain.model.ClassModel(1L, "Software Engineering", "6C1")
        override suspend fun insertClass(classModel: com.attendance.app.domain.model.ClassModel) = 0L
        override suspend fun updateClass(classModel: com.attendance.app.domain.model.ClassModel) {}
        override suspend fun deleteClass(classModel: com.attendance.app.domain.model.ClassModel) {}
    },
    attendanceRepository = object : com.attendance.app.domain.repository.AttendanceRepository {
        override fun getAttendanceByClassAndDate(classId: Long, date: String) = error("Not implemented")
        override fun getAttendanceByStudent(studentId: Long, classId: Long) = kotlinx.coroutines.flow.flowOf(
            listOf(
                com.attendance.app.domain.model.AttendanceRecord(1, 1, 1, "2024-04-03", AttendanceStatus.PRESENT),
                com.attendance.app.domain.model.AttendanceRecord(2, 1, 1, "2024-04-02", AttendanceStatus.PRESENT),
                com.attendance.app.domain.model.AttendanceRecord(3, 1, 1, "2024-04-01", AttendanceStatus.ABSENT)
            )
        )
        override suspend fun saveAttendance(records: List<com.attendance.app.domain.model.AttendanceRecord>) {}
        override fun getSessionSummary(classId: Long, date: String) = error("Not implemented")
        override fun getRecentSessions(classId: Long, limit: Int) = error("Not implemented")
        override fun getSessionDates(classId: Long) = error("Not implemented")
        override suspend fun getAllAttendanceForClass(classId: Long) = emptyList<com.attendance.app.domain.model.AttendanceRecord>()
        override suspend fun getAllAttendance() = emptyList<com.attendance.app.domain.model.AttendanceRecord>()
    },
    savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("studentId" to 1L, "classId" to 1L))
)

@Preview(showBackground = true)
@Composable
fun InfoSectionPreview() {
    AttendanceTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DetailRow(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Enrollment Date",
                    value = "April 03, 2024"
                )
                Spacer(modifier = Modifier.height(20.dp))
                DetailRow(
                    icon = Icons.Rounded.School,
                    label = "Assigned Class",
                    value = "Software Engineering (6C1)"
                )
            }
        }
    }
}
