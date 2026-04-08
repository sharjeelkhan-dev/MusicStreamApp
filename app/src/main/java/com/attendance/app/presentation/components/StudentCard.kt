package com.attendance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.presentation.theme.*

@Composable
fun StudentCard(
    initials: String,
    name: String,
    rollNumber: String,
    attendancePercentage: Double,
    avatarColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = AvatarTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and roll number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = rollNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Attendance percentage badge
            val percentageColor = when {
                attendancePercentage >= 80 -> PresentGreen
                attendancePercentage >= 50 -> LateOrange
                else -> AbsentRed
            }
            val percentageBgColor = percentageColor.copy(alpha = 0.12f)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(percentageBgColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${attendancePercentage.toInt()}%",
                    color = percentageColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StudentAttendanceItem(
    initials: String,
    name: String,
    rollNumber: String,
    attendancePercentage: Double,
    avatarColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = AvatarTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Percentage text
        val percentageColor = when {
            attendancePercentage >= 80 -> PresentGreen
            attendancePercentage >= 50 -> LateOrange
            else -> AbsentRed
        }

        Text(
            text = "${attendancePercentage.toInt()}%",
            color = percentageColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }

    // Progress bar
    val progressColor = when {
        attendancePercentage >= 80 -> PresentGreen
        attendancePercentage >= 50 -> LateOrange
        else -> AbsentRed
    }

    LinearProgressIndicator(
        progress = { (attendancePercentage / 100f).toFloat() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(4.dp)
            .clip(CircleShape),
        color = progressColor,
        trackColor = progressColor.copy(alpha = 0.1f),
    )
}

@Preview
@Composable
fun StudentCardPreview() {
    AttendanceTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StudentCard(
                initials = "JD",
                name = "John Doe",
                rollNumber = "CS-001",
                attendancePercentage = 85.0,
                avatarColor = Color(0xFF42A5F5)
            )
            
            StudentAttendanceItem(
                initials = "JS",
                name = "Jane Smith",
                rollNumber = "CS-002",
                attendancePercentage = 45.0,
                avatarColor = Color(0xFFEF5350)
            )
        }
    }
}
