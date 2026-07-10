package com.attendance.app.presentation.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
    showSearch: Boolean = false,
    showDate: Boolean = false,
    date: String? = null,
    onDateClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    showAi: Boolean = false,
    onAiClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    showSave: Boolean = false,
    onSaveClick: () -> Unit = {},
    isSaving: Boolean = false,
    isSaved: Boolean = false,
    showExpand: Boolean = false,
    isExpanded: Boolean = false,
    onExpandClick: () -> Unit = {}
) {
    val backgroundColor = PrimaryGreenDark
    val contentColor = Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.85f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .height(70.dp) // Strictly maintained 70.dp height
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Top section for Back / Date / Save / Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp), // Reduced from 26.dp to save space
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Back button OR Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(24.dp).offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (showDate) {
                    val localDate = try {
                        if (date != null) LocalDate.parse(date) else LocalDate.now()
                    } catch (_: Exception) {
                        LocalDate.now()
                    }
                    val dateFormatted = localDate.format(
                        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)
                    )
                    Surface(
                        onClick = { onDateClick?.invoke() },
                        color = Color.Transparent,
                        enabled = onDateClick != null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryContentColor,
                                fontSize = 12.sp
                            )
                            if (onDateClick != null) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = secondaryContentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right side: Save / Search / Settings / Expand
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showExpand) {
                    IconButton(
                        onClick = onExpandClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Details",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (showSave) {
                    Surface(
                        onClick = onSaveClick,
                        enabled = !isSaving && !isSaved,
                        shape = RoundedCornerShape(100),
                        color = Color.White,
                        modifier = Modifier.height(20.dp).offset(y = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    color = PrimaryGreenDark,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isSaved) "Saved" else "Save",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = PrimaryGreenDark
                                )
                                if (isSaved) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = PrimaryGreenDark
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSearch) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showAi) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onAiClick,
                        modifier = Modifier.size(24.dp).offset(y = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showSettings) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(24.dp).offset(y = 6.dp)
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

        // Title and Subtitle with original sizes,
        // but removed negative offsets to prevent clipping
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp, // Kept original size
            lineHeight = 28.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

@Preview(name = "Student Detail Header")
@Composable
fun PreviewStudentDetailHeader() {
    AttendanceTheme(darkTheme = false) {
        StandardHeader(
            title = "Student Details",
            subtitle = "2233779",
            onBackClick = {}
        )
    }
}
