package com.attendance.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    studentNames: List<Pair<String, Boolean>> = emptyList()
) {
    val parsedDate = try {
        LocalDate.parse(date)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val displayMonthDay = parsedDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH))
    val displayDayOfWeek = parsedDate.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))

    val effectiveStatus = status ?: if (percentage >= 80) "Good" else "Fair"
    
    val accentColor = when {
        percentage >= 80 -> PresentGreen
        percentage >= 50 -> LateOrange
        else -> AbsentRed
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayDayOfWeek,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                trackColor = accentColor.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom row: Avatars
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    val sortedNames = remember(studentNames) {
                        studentNames.sortedBy { !it.second }
                    }
                    val namesToShow = if (sortedNames.isNotEmpty()) {
                        sortedNames.take(6)
                    } else {
                        // Fallback dummy data
                        listOf(
                            "Aisha Khan" to true, 
                            "Bilal Ahmed" to true, 
                            "Fatima Malik" to false, 
                            "Hamza Ali" to true, 
                            "Ira Naeem" to true, 
                            "Junaid Rashid" to false
                        ).sortedBy { !it.second }
                    }
                    
                    namesToShow.forEach { (name, isPresent) ->
                        val initials = name.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(2.dp)
                                .background(
                                    if (isPresent) getAvatarColor(name)
                                    else if (LocalIsDarkMode.current) Color(0xFF2C2C2C) else Color(0xFFF5F5F5),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPresent) AvatarTextColor else getAvatarColor(name)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionsSummaryCard(
    sessionCount: Int,
    recentPercentages: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.offset(x = (10).dp),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sessionCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(x = (10).dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "sessions",
                        modifier = Modifier.offset(x = (10).dp, y = 3.2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Daily attendance",
                    modifier = Modifier.offset(x = (-10).dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.offset(x = (-10).dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Show last 5 percentages as bars
                    val displayPercentages = if (recentPercentages.isEmpty()) {
                        listOf(45, 80, 65, 90, 85) // Placeholder if no data
                    } else {
                        recentPercentages.takeLast(5).let { list ->
                            if (list.size < 5) {
                                List(5 - list.size) { 0 } + list
                            } else list
                        }
                    }

                    displayPercentages.forEach { pct ->
                        val barHeight = (8 + (pct * 0.18)).dp // Scale height based on percentage
                        val color = when {
                            pct >= 80 -> Color(0xFF00BFA5) // PresentGreen/Teal
                            pct >= 50 -> LateOrange
                            else -> AbsentRed
                        }
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (pct > 0) color else color.copy(alpha = 0.1f))
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
    AttendanceTheme(darkTheme = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp).fillMaxSize()) {
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
