package com.attendance.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    totalCount: Int,
    percentage: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val parsedDate = try {
        LocalDate.parse(date)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val displayDate = parsedDate.format(
        DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH)
    )

    val percentageColor = when {
        percentage >= 80 -> PresentGreen
        percentage >= 50 -> LateOrange
        else -> AbsentRed
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date and count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$presentCount / $totalCount present",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                )
            }

            // Percentage
            Text(
                text = "$percentage%",
                color = percentageColor,
                modifier = Modifier.offset(x = 72.dp, y = (-13).dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Mini progress bar
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = percentageColor,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Preview
@Composable
fun SessionCardPreview() {
    AttendanceTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SessionCard(
                date = "2024-05-20",
                presentCount = 38,
                totalCount = 45,
                percentage = 84
            )
        }
    }
}
