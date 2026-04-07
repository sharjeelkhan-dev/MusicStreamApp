package com.attendance.app.presentation.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.attendance.app.domain.model.AttendanceStatus
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.Student
import com.attendance.app.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Fixed Header
        AttendanceHeader(
            className = state.selectedClass?.name ?: "No Class",
            presentCount = state.presentCount,
            absentCount = state.absentCount,
            isSaving = state.isSaving,
            isSaved = state.isSaved,
            onSave = { onEvent(AttendanceEvent.SaveAttendance) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(AttendanceEvent.SearchQueryChanged(it)) },
                    placeholder = { Text("Search student...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = PrimaryGreen
                    ),
                    singleLine = true
                )
            }

            // All Present / All Absent buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEvent(AttendanceEvent.MarkAllPresent) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PresentGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PresentGreen)
                    ) {
                        Text("All Present", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { onEvent(AttendanceEvent.MarkAllAbsent) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AbsentRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AbsentRed)
                    ) {
                        Text("All Absent", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Student list
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
}

@Composable
private fun AttendanceHeader(
    className: String,
    presentCount: Int,
    absentCount: Int,
    isSaving: Boolean,
    isSaved: Boolean,
    onSave: () -> Unit
) {
    val today = LocalDate.now()
    val dateFormatted = today.format(
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryGreenDark)
            .statusBarsPadding()
            .height(115.dp)
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dateFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Take Attendance",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$className · $presentCount Present · $absentCount Absent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onSave,
                enabled = !isSaving,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryGreen
                )
            ) {
                Text(
                    text = if (isSaved) "Saved" else "Save",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (isSaved) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceStudentRow(
    studentState: StudentAttendanceState,
    avatarColor: Color,
    onToggle: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = studentState.student.rollNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Present button
            val isPresentSelected = studentState.status == AttendanceStatus.PRESENT
            Button(
                onClick = { onToggle(AttendanceStatus.PRESENT) },
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPresentSelected) PresentGreen else PresentGreen.copy(alpha = 0.1f),
                    contentColor = if (isPresentSelected) Color.White else PresentGreen
                )
            ) {
                Text("P", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Absent button
            val isAbsentSelected = studentState.status == AttendanceStatus.ABSENT
            Button(
                onClick = { onToggle(AttendanceStatus.ABSENT) },
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAbsentSelected) AbsentRed else AbsentRed.copy(alpha = 0.1f),
                    contentColor = if (isAbsentSelected) Color.White else AbsentRed
                )
            ) {
                Text("A", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TakeAttendancePreview() {
    AttendanceTheme {
        AttendanceContent(
            state = AttendanceState(
                selectedClass = ClassModel(id = 1, name = "Computer Science", section = "A"),
                students = listOf(
                    StudentAttendanceState(
                        student = Student(id = 1, fullName = "John Doe", rollNumber = "CS001", classId = 1),
                        status = AttendanceStatus.PRESENT
                    ),
                    StudentAttendanceState(
                        student = Student(id = 2, fullName = "Jane Smith", rollNumber = "CS002", classId = 1),
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
