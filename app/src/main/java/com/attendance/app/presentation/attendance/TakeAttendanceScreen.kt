package com.attendance.app.presentation.attendance
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.Student
import com.attendance.app.presentation.components.StandardHeader
import com.attendance.app.presentation.components.VerticalScrollbar
import com.attendance.app.presentation.theme.*

@Composable
fun TakeAttendanceScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttendanceContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        paddingValues = paddingValues
    )
}

@Composable
private fun AttendanceContent(
    state: AttendanceState,
    onEvent: (AttendanceEvent) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val filteredStudents = if (state.searchQuery.isBlank()) {
        state.students
    } else {
        state.students.filter {
            it.student.fullName.contains(state.searchQuery, ignoreCase = true) ||
            it.student.rollNumber.contains(state.searchQuery, ignoreCase = true)
        }
    }

    val isDark = LocalIsDarkMode.current
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
            .background(MaterialTheme.colorScheme.background)
    ) {
        StandardHeader(
            title = "Take Attendance",
            subtitle = "${state.selectedClass?.name ?: "No Class"} · ${state.presentCount} Present · ${state.absentCount} Absent",
            showDate = true,
            showSave = true,
            onSaveClick = { onEvent(AttendanceEvent.SaveAttendance) },
            isSaving = state.isSaving,
            isSaved = state.isSaved
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                // Search Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Bar
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color(0xFFF2F4F7),
                            tonalElevation = 2.dp,
                            border = if (isDark) 
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) 
                            else 
                                androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
                        ) {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = { onEvent(AttendanceEvent.SearchQueryChanged(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "Search student...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    ) 
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp).offset(x = 5.dp)
                                    ) 
                                },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onEvent(AttendanceEvent.SearchQueryChanged("")) }) {
                                            Icon(
                                                Icons.Default.Close, 
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Student list - Simple Rows without Cards
                if (filteredStudents.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.searchQuery.isBlank())
                                    "No students found in this class." +
                                            "\nAdd students to start taking attendance!"
                                else
                                    "No students match your search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    items(filteredStudents, key = { it.student.id }) { studentState ->
                        AttendanceStudentRow(
                            studentState = studentState,
                            avatarColor = getAvatarColor(studentState.student.fullName),
                            onToggle = { status ->
                                onEvent(AttendanceEvent.ToggleStatus(studentState.student.id, status))
                            }
                        )
                    }
                }
            }

            VerticalScrollbar(
                lazyListState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceStudentRow(
    studentState: StudentAttendanceState,
    avatarColor: Color,
    onToggle: (AttendanceStatus) -> Unit
) {
    val isDark = LocalIsDarkMode.current

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = Color.Gray.copy(alpha = 0.15f)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = { /* Could navigate or show details */ }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = studentState.student.initials,
                        color = AvatarTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and roll
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentState.student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = studentState.student.rollNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Present button
                val isPresentSelected = studentState.status == AttendanceStatus.PRESENT
                Button(
                    onClick = { onToggle(AttendanceStatus.PRESENT) },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPresentSelected) PresentGreen else if (isDark) DividerColorDark else PresentGreen.copy(alpha = 0.1f),
                        contentColor = if (isPresentSelected) Color.White else PresentGreen
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isPresentSelected) 4.dp else 0.dp)
                ) {
                    Text("P", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Absent button
                val isAbsentSelected = studentState.status == AttendanceStatus.ABSENT
                Button(
                    onClick = { onToggle(AttendanceStatus.ABSENT) },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAbsentSelected) AbsentRed else if (isDark) DividerColorDark else AbsentRed.copy(alpha = 0.1f),
                        contentColor = if (isAbsentSelected) Color.White else AbsentRed
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isAbsentSelected) 4.dp else 0.dp)
                ) {
                    Text("A", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}



@Preview(showBackground = true, name = "Light Mode")
@Composable
fun TakeAttendancePreviewLight() {
    AttendanceTheme(darkTheme = false) {
        AttendanceContent(
            state = AttendanceState(
                selectedClass = ClassModel(id = 1, name = "Software Engineering", section = "6C1"),
                students = listOf(
                    StudentAttendanceState(
                        student = Student(id = 1, fullName = "John Doe", rollNumber = "2021-SE-01", classId = 1),
                        status = AttendanceStatus.PRESENT
                    ),
                    StudentAttendanceState(
                        student = Student(id = 2, fullName = "Jane Smith", rollNumber = "2021-SE-02", classId = 1),
                        status = AttendanceStatus.ABSENT
                    ),
                    StudentAttendanceState(
                        student = Student(id = 3, fullName = "Alex Johnson", rollNumber = "2021-SE-03", classId = 1),
                        status = AttendanceStatus.PRESENT
                    ),
                    StudentAttendanceState(
                        student = Student(id = 4, fullName = "Sarah Williams", rollNumber = "2021-SE-04", classId = 1),
                        status = AttendanceStatus.PRESENT
                    )
                ),
                presentCount = 3,
                absentCount = 1,
                isLoading = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TakeAttendancePreviewDark() {
    AttendanceTheme(darkTheme = true) {
        AttendanceContent(
            state = AttendanceState(
                selectedClass = ClassModel(id = 1, name = "Software Engineering", section = "6C1"),
                students = listOf(
                    StudentAttendanceState(
                        student = Student(id = 1, fullName = "John Doe", rollNumber = "2021-SE-01", classId = 1),
                        status = AttendanceStatus.PRESENT
                    ),
                    StudentAttendanceState(
                        student = Student(id = 2, fullName = "Jane Smith", rollNumber = "2021-SE-02", classId = 1),
                        status = AttendanceStatus.ABSENT
                    )
                ),
                presentCount = 1,
                absentCount = 1,
                isLoading = false
            ),
            onEvent = {}
        )
    }
}
