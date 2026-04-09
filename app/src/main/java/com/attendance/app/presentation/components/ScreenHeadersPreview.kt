package com.attendance.app.presentation.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
    onBackClick: (() -> Unit)? = null,
    showSettings: Boolean = false,
    showDate: Boolean = false,
    onSettingsClick: () -> Unit = {},
    showSave: Boolean = false,
    onSaveClick: () -> Unit = {},
    isSaving: Boolean = false,
    isSaved: Boolean = false
) {
    val backgroundColor = PrimaryGreenDark
    val contentColor = Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.85f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .height(95.dp) // Optimized height for high-quality feel
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Top section for Back / Date / Save / Settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp), // Fixed height to keep title alignment consistent
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Back button OR Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(24.dp).offset(x = (-8).dp) // Slight offset for better alignment
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (showDate) {
                        val dateFormatted = LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
                        )
                        Text(
                            text = dateFormatted,
                            modifier = Modifier.offset(y = 5.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryContentColor,
                            fontSize = 12.sp
                        )
                    }
                }

                // Right side: Save / Settings
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showSave) {
                        Surface(
                            onClick = onSaveClick,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(100),
                            color = Color.White,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = PrimaryGreenDark,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isSaved) "Saved" else "Save",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = PrimaryGreenDark
                                    )
                                    if (isSaved) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = PrimaryGreenDark
                                        )
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
                                modifier = Modifier.size(18.dp)
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
            fontSize = 27.sp,
            lineHeight = 32.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
    }
}

@Preview(name = "Take Attendance Header")
@Composable
fun PreviewTakeAttendanceHeader() {
    AttendanceTheme(darkTheme = false) {
        StandardHeader(
            title = "Take Attendance",
            subtitle = "Computer Science · 1 Present · 1 Absent",
            showDate = true,
            showSave = true
        )
    }
}

@Preview(name = "Home Header")
@Composable
fun PreviewHomeHeader() {
    AttendanceTheme(darkTheme = false) {
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
    AttendanceTheme(darkTheme = false) {
        StandardHeader(
            title = "Your Classes",
            subtitle = "5 classes total"
        )
    }
}

@Preview(name = "Students Header")
@Composable
fun PreviewStudentsHeader() {
    AttendanceTheme(darkTheme = false) {
        StandardHeader(
            title = "Students",
            subtitle = "Software Engineering — 6C1"
        )
    }
}

@Preview(name = "Reports Header")
@Composable
fun PreviewReportsHeader() {
    AttendanceTheme(darkTheme = false) {
        StandardHeader(
            title = "Attendance Report",
            subtitle = "Software Engineering — 6C1"
        )
    }
}
