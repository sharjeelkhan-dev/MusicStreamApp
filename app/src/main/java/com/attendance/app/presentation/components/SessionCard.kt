package com.attendance.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SessionCard(
    date: String,
    presentCount: Int,
    absentCount: Int,
    percentage: Int,
    modifier: Modifier = Modifier,
    status: String? = null,
    studentNames: List<String> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    val parsedDate = try {
        LocalDate.parse(date)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val displayMonthDay = parsedDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH))
    val displayDayOfWeek = parsedDate.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))

    val isToday = parsedDate == LocalDate.now()
    val effectiveStatus = status ?: if (percentage >= 80) "Good" else "Fair"
    
    val accentColor = when {
        percentage >= 80 -> Color(0xFF4CAF50) // Green
        percentage >= 50 -> Color(0xFFFFA726) // Orange
        else -> Color(0xFFE53935) // Red
    }

    val borderColor = Color.LightGray.copy(alpha = 0.3f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                
                Spacer(modifier = Modifier.width(12.dp))

                // Date
                Text(
                    text = displayMonthDay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayDayOfWeek,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status Badge
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = effectiveStatus,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Circular Progress
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                    CircularProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.1f),
                        strokeWidth = 3.5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Attendance text
            Text(
                text = "$presentCount present  ·  $absentCount absent",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 22.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Linear Progress
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 12.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = Color(0xFFF0F0F0),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom row: Avatars and Arrow
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    val namesToShow = if (studentNames.isNotEmpty()) {
                        studentNames.take(6)
                    } else {
                        listOf("Aisha Khan", "Bilal Ahmed", "Fatima Malik", "Hamza Ali", "Ira Naeem", "Junaid Rashid")
                    }
                    
                    namesToShow.forEachIndexed { index, name ->
                        val initials = name.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                                .background(getAvatarColor(name), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AvatarTextColor
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SessionsSummaryCard(
    sessionCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sessionCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Daily attendance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Simple bar chart representation matching the image
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = listOf(
                        10.dp to Color(0xFFFFA726), // Orange
                        18.dp to Color(0xFF00BFA5), // Teal
                        14.dp to Color(0xFF00BFA5),
                        22.dp to Color(0xFF00BFA5),
                        18.dp to Color(0xFF00BFA5)
                    )
                    bars.forEach { (height, color) ->
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(height)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SessionCardPreview() {
    AttendanceTheme {
        Column(modifier = Modifier.background(Color(0xFFF8F9FE)).padding(16.dp).fillMaxSize()) {
            SessionsSummaryCard(sessionCount = 3)
            Spacer(modifier = Modifier.height(16.dp))
            SessionCard(
                date = "2024-05-15",
                presentCount = 40,
                absentCount = 5,
                percentage = 88
            )
        }
    }
}
