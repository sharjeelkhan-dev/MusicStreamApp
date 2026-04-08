package com.attendance.app.presentation.students

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.R
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.domain.model.Student
import com.attendance.app.presentation.components.StandardHeader
import com.attendance.app.presentation.components.VerticalScrollbar
import com.attendance.app.presentation.theme.*

@Composable
fun StudentsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: StudentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(StudentsEvent.ClearError)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(StudentsEvent.ClearSuccess)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        StudentsContent(
            state = state,
            onBack = onBack,
            onEvent = viewModel::onEvent,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun StudentsContent(
    state: StudentsState,
    onBack: () -> Unit,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val isDark = LocalIsDarkMode.current
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Fixed Header
        StandardHeader(
            title = "Students",
            subtitle = state.selectedClass?.let {
                "${it.name} \u2014 ${it.section}"
            } ?: "No Class Selected"
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 80.dp // Extra padding for FAB
                )
            ) {
                // Enrolled count
                if (state.selectedClass != null) {
                    item {
                        Text(
                            text = "${state.students.size} ENROLLED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }

                if (state.students.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.selectedClass == null) 
                                    "Please select a class first to manage students."
                                else 
                                    "No students enrolled yet.\nAdd your first student above!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        state.students.sortedByDescending { it.student.createdAt }, 
                        key = { it.student.id }
                    ) { studentWithPct ->
                        StudentRow(
                            initials = studentWithPct.student.initials,
                            name = studentWithPct.student.fullName,
                            rollNumber = studentWithPct.student.rollNumber,
                            attendancePercentage = studentWithPct.attendancePercentage,
                            createdAt = studentWithPct.student.createdAt,
                            avatarColor = getAvatarColor(studentWithPct.student.fullName),
                            onEdit = { onEvent(StudentsEvent.StartEditStudent(studentWithPct.student)) },
                            onDelete = { onEvent(StudentsEvent.DeleteStudent(studentWithPct.student)) }
                        )
                    }
                }
            }

            VerticalScrollbar(
                lazyListState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
                )
            }

            // Floating Action Button at Bottom Right
            if (state.selectedClass != null) {
                val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
                FloatingActionButton(
                    onClick = { onEvent(StudentsEvent.ToggleAddForm) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = paddingValues.calculateBottomPadding() + 24.dp, end = 24.dp),
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Student", modifier = Modifier.size(28.dp))
                }
            }

            // Pop-up Add Form
            if (state.isAddFormVisible) {
                Dialog(
                    onDismissRequest = { 
                        if (state.editingStudent != null) onEvent(StudentsEvent.CancelEdit)
                        else onEvent(StudentsEvent.ToggleAddForm)
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        AddStudentForm(
                            name = state.newStudentName,
                            rollNumber = state.newStudentRoll,
                            isEditing = state.editingStudent != null,
                            isSaving = state.isSaving,
                            onNameChange = { onEvent(StudentsEvent.UpdateNewName(it)) },
                            onRollChange = { onEvent(StudentsEvent.UpdateNewRoll(it)) },
                            onSubmit = {
                                if (state.editingStudent != null)
                                    onEvent(StudentsEvent.SaveEdit)
                                else
                                    onEvent(StudentsEvent.AddStudent)
                            },
                            onCancel = {
                                if (state.editingStudent != null)
                                    onEvent(StudentsEvent.CancelEdit)
                                else
                                    onEvent(StudentsEvent.ToggleAddForm)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStudentForm(
    name: String,
    rollNumber: String,
    isEditing: Boolean,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onRollChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = if (isEditing) "Edit Student" else "Add New Student",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            placeholder = { Text("e.g. John Doe") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                focusedLabelColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
            ),
            singleLine = true,
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = rollNumber,
            onValueChange = onRollChange,
            label = { Text("Roll Number") },
            placeholder = { Text("e.g. CS-09") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                focusedLabelColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
            ),
            singleLine = true,
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF37474F) else Color(0xFFE0E0E0),
                    contentColor = if (isDark) Color.White else Color(0xFF424242)
                )
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), 
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (isEditing) "Save" else "Add Student",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentRow(
    initials: String,
    name: String,
    rollNumber: String,
    attendancePercentage: Double,
    createdAt: Long,
    avatarColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = AvatarTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rollNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )

                    val enrolledDate = remember(createdAt) {
                        java.time.Instant.ofEpochMilli(createdAt)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    }

                    Text(
                        text = "Enrolled $enrolledDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Edit
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.pencil_circle),
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.recycle_bin_icon),
                    contentDescription = "Delete",
                    tint = AbsentRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentsPreview() {
    AttendanceTheme {
        StudentsContent(
            state = StudentsState(
                selectedClass = ClassModel(name = "Software Engineering", section = "6C1"),
                students = listOf(
                    StudentWithPercentage(
                        student = Student(id = 1, fullName = "John Doe", rollNumber = "101", classId = 1),
                        attendancePercentage = 85.0
                    )
                ),
                isLoading = false
            ),
            onBack = {},
            onEvent = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}
