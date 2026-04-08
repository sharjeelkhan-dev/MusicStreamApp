package com.attendance.app.presentation.classes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.attendance.app.R
import com.attendance.app.domain.model.ClassModel
import com.attendance.app.presentation.components.StandardHeader
import com.attendance.app.presentation.theme.*

@Composable
fun ClassesScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ClassesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClassesContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        paddingValues = paddingValues
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassesContent(
    state: ClassesState,
    onEvent: (ClassesEvent) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val isDark = LocalIsDarkMode.current
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF5F7FA)
    
    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Header
            StandardHeader(
                title = "Your Classes",
                subtitle = "${state.classes.size} classes total"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 80.dp // Extra padding for FAB
                )
            ) {
                // Classes Header
                item {
                    Text(
                        text = "YOUR CLASSES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                if (state.classes.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No classes yet.\nCreate your first class to get started!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(state.classes, key = { it.id }) { classModel ->
                        val isSelected = classModel.id == state.selectedClassId
                        ClassRow(
                            classModel = classModel,
                            isSelected = isSelected,
                            onSelect = { onEvent(ClassesEvent.SelectClass(classModel)) },
                            onEdit = { onEvent(ClassesEvent.StartEdit(classModel)) },
                            onDelete = { onEvent(ClassesEvent.DeleteClass(classModel)) }
                        )
                    }
                }
            }
        }

        // Floating Action Button at Bottom Right
        val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
        FloatingActionButton(
            onClick = { onEvent(ClassesEvent.ToggleAddForm) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = paddingValues.calculateBottomPadding() + 24.dp, end = 24.dp),
            containerColor = primaryColor,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Class", modifier = Modifier.size(28.dp))
        }

        // Pop-up Add Form
        if (state.isAddFormVisible) {
            Dialog(
                onDismissRequest = { 
                    if (state.editingClass != null) onEvent(ClassesEvent.CancelEdit)
                    else onEvent(ClassesEvent.ToggleAddForm)
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = if (state.editingClass != null) "Edit Class" else "Add New Class",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = state.newClassName,
                            onValueChange = { onEvent(ClassesEvent.UpdateClassName(it)) },
                            label = { Text("Class Name") },
                            placeholder = { Text("e.g. Data Structures") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                focusedBorderColor = primaryColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                focusedLabelColor = primaryColor
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = state.newClassSection,
                            onValueChange = { onEvent(ClassesEvent.UpdateClassSection(it)) },
                            label = { Text("Section") },
                            placeholder = { Text("e.g. Section A") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                focusedBorderColor = primaryColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                focusedLabelColor = primaryColor
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (state.editingClass != null) onEvent(ClassesEvent.CancelEdit)
                                    else onEvent(ClassesEvent.ToggleAddForm)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF37474F) else Color(0xFFE0E0E0),
                                    contentColor = if (isDark) Color.White else Color(0xFF424242)
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    if (state.editingClass != null) onEvent(ClassesEvent.SaveEdit)
                                    else onEvent(ClassesEvent.AddClass)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text(
                                    if (state.editingClass != null) "Save" else "Add Class",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassRow(
    classModel: ClassModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
    
    val containerColor = if (isSelected) primaryColor else (if (isDark) MaterialTheme.colorScheme.surface else Color.White)
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent Dot for consistency
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else primaryColor.copy(alpha = 0.3f))
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classModel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (classModel.section.isNotBlank()) {
                        Text(
                            text = classModel.section,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryContentColor
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryContentColor.copy(alpha = 0.5f)
                        )
                    }
                    
                    val dateFormatted = remember(classModel.createdAt) {
                        java.time.Instant.ofEpochMilli(classModel.createdAt)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    }
                    
                    Text(
                        text = "Created $dateFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContentColor,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.pencil_circle),
                    contentDescription = "Edit",
                    tint = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.recycle_bin_icon),
                    contentDescription = "Delete",
                    tint = if (isSelected) Color.White.copy(alpha = 0.9f)
                    else AbsentRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Classes List")
@Composable
fun ClassesPreview() {
    AttendanceTheme(darkTheme = false) {
        ClassesContent(
            state = ClassesState(
                classes = listOf(
                    ClassModel(id = 1, name = "Computer Science", section = "Section A"),
                    ClassModel(id = 2, name = "Mathematics", section = "Section B")
                ),
                selectedClassId = 1,
                isLoading = false
            ),
            onEvent = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true, name = "Add Class Form")
@Composable
fun AddClassPreview() {
    AttendanceTheme(darkTheme = false) {
        ClassesContent(
            state = ClassesState(
                classes = emptyList(),
                isAddFormVisible = true,
                newClassName = "Mobile Development",
                newClassSection = "6th Semester",
                isLoading = false
            ),
            onEvent = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true, name = "Empty Classes")
@Composable
fun EmptyClassesPreview() {
    AttendanceTheme(darkTheme = false) {
        ClassesContent(
            state = ClassesState(
                classes = emptyList(),
                isLoading = false
            ),
            onEvent = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}
