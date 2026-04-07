package com.attendance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.R
import com.attendance.app.presentation.theme.AttendanceTheme
import com.attendance.app.presentation.theme.PrimaryGreenDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StandardHeader(
    title: String,
    subtitle: String,
    showSettings: Boolean = false,
    showDate: Boolean = false,
    onSettingsClick: () -> Unit = {},
    showSave: Boolean = false,
    onSaveClick: () -> Unit = {},
    isSaving: Boolean = false,
    isSaved: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.surface else PrimaryGreenDark
    val contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
    val secondaryContentColor = contentColor.copy(alpha = if (isDark) 0.7f else 0.85f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .height(115.dp)
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (showDate || showSettings || showSave) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDate) {
                    val dateFormatted = LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContentColor,
                        fontSize = 13.sp
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showSave) {
                        Card(
                            onClick = onSaveClick,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f) 
                                                else Color.White.copy(alpha = 0.2f),
                                contentColor = contentColor
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = contentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isSaved) "Saved" else "Save",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                        if (isSaved) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showSettings) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.setting_icon),
                                contentDescription = "Settings",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

@Preview(name = "Home Header")
@Composable
fun PreviewHomeHeader() {
    AttendanceTheme {
        StandardHeader(
            title = "Good Morning 👋",
            subtitle = "Software Engineering — 6C1",
            showDate = true,
            showSettings = true
        )
    }
}

@Preview(name = "Classes Header")
@Composable
fun PreviewClassesHeader() {
    AttendanceTheme {
        StandardHeader(
            title = "Your Classes",
            subtitle = "5 classes total"
        )
    }
}

@Preview(name = "Students Header")
@Composable
fun PreviewStudentsHeader() {
    AttendanceTheme {
        StandardHeader(
            title = "Students",
            subtitle = "Software Engineering — 6C1"
        )
    }
}

@Preview(name = "Reports Header")
@Composable
fun PreviewReportsHeader() {
    AttendanceTheme {
        StandardHeader(
            title = "Attendance Report",
            subtitle = "Software Engineering — 6C1"
        )
    }
}
