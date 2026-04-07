package com.attendance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    onSettingsClick: () -> Unit = {}
) {
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
        if (showDate || showSettings) {
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
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                if (showSettings) {
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
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
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
