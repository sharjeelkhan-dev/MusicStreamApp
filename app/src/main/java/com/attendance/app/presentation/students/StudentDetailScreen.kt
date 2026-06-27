package com.attendance.app.presentation.students

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.presentation.theme.*
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StudentDetailScreen(
    studentName: String,
    studentRoll: String,
    initials: String,
    avatarColor: Color,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val isDark = LocalIsDarkMode.current

    // Refresh data on entry for real-time update
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF8F9FA))
    ) {
        // EXACT Header matching Settings Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryGreenDark)
                .statusBarsPadding()
                .height(70.dp)
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Text Content (Exactly as Settings Screen)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 65.dp, top = 12.dp)
            ) {
                Text(
                    text = "Student Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 25.sp, // Kept original size
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = studentRoll,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp, // Kept original size
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            // Professional Orange Avatar
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE65100)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Name and Roll
            Text(
                text = studentName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = "Roll Number: $studentRoll",
                fontSize = 15.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(35.dp))

            // Professional Stat Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    value = String.format(Locale.US, "%02d", state.presentCount),
                    label = "Present",
                    valueColor = PresentGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = String.format(Locale.US, "%02d", state.absentCount),
                    label = "Absent",
                    valueColor = AbsentRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${state.attendancePercentage.toInt()}%",
                    label = "Attendance",
                    valueColor = Color(0xFF0288D1),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(35.dp))

            // Clean Attendance Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE).copy(alpha = if (isDark) 0.1f else 1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) Color.White else Color.Black
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChartLegend(color = AbsentRed, label = "you")
                            Spacer(modifier = Modifier.width(16.dp))
                            ChartLegend(color = Color(0xFF42A5F5), label = "class avg")
                        }
                    }

                    Spacer(modifier = Modifier.height(35.dp))

                    AttendanceDualBarChart(
                        studentData = state.weeklyStudentData,
                        classAvgData = state.weeklyClassAvgData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Professional Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE).copy(alpha = if (isDark) 0.1f else 1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val enrollmentDate = "April 11, 2026"

                    InfoRow(
                        label = "Enrollment Date",
                        value = enrollmentDate,
                        icon = Icons.Default.CalendarToday
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    val className = state.classModel?.let { "${it.name} (${it.section})" } ?: "DSA (6C1)"
                    InfoRow(
                        label = "Assigned Class",
                        value = className,
                        icon = Icons.Default.School
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark)
            MaterialTheme.colorScheme.surface
        else Color.White),
        elevation = CardDefaults.cardElevation
            (defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 15.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun AttendanceDualBarChart(
    studentData: List<Float>,
    classAvgData: List<Float>,
    modifier: Modifier = Modifier
) {
    val yLabels = listOf("100%", "75%", "50%", "25%", "0%")
    val xAxisHeight = 35.dp
    val labelWidth = 35.dp
    val labelSpacer = 10.dp
    val rowHeight = 16.dp
    
    Box(modifier = modifier) {
        // Main Background (Labels and Grid Lines perfectly aligned)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = xAxisHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yLabels.forEach { label ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(rowHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(labelWidth).offset(y = (-5).dp)
                    )
                    Spacer(modifier = Modifier.width(labelSpacer))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.Gray.copy(alpha = 0.1f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bars Layer (Overlaid precisely on grid)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = labelWidth + labelSpacer, bottom = xAxisHeight)
                .padding(vertical = rowHeight / 2) // Match divider centers
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4")
                weeks.forEachIndexed { index, _ ->
                    val sValRaw = (studentData.getOrNull(index) ?: 0f).coerceIn(1f, 100f)
                    val cValRaw = (classAvgData.getOrNull(index) ?: 0f).coerceIn(1f, 100f)

                    val sVal by animateFloatAsState(
                        targetValue = sValRaw / 100f,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "studentBar"
                    )
                    val cVal by animateFloatAsState(
                        targetValue = cValRaw / 100f,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "classBar"
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Student bar (Red)
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(sVal)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(AbsentRed)
                        )
                        // Class avg bar (Blue)
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(cVal)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(Color(0xFF42A5F5))
                        )
                    }
                }
            }
        }

        // X-axis labels (Weeks)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .padding(start = labelWidth + labelSpacer)
                .height(xAxisHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4")
            weeks.forEach { week ->
                Text(
                    text = week,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp).offset(y = (-23).dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(y = 2.dp)
                .clip(CircleShape)
                .background(PrimaryGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = if (LocalIsDarkMode.current) Color.White else Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentDetailScreenPreview() {
    AttendanceTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
            // EXACT Header matching Settings Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryGreenDark)
                    .statusBarsPadding()
                    .height(70.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Text Content
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 65.dp, top = 12.dp)
                ) {
                    Text(
                        text = "Student Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontSize = 25.sp, // Kept original size
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "2233779",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp, // Kept original size
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(35.dp))

                Box(
                    modifier = Modifier.size(130.dp).clip(CircleShape)
                        .background(Color(0xFFE65100)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ZT", color = Color.White, fontSize = 50.sp,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))

                Text("Zain Tanveer", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Roll Number: 2233779", fontSize = 15.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(35.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatCard("06", "Present", PresentGreen,
                        Modifier.weight(1f))
                    StatCard("01", "Absent", AbsentRed,
                        Modifier.weight(1f))
                    StatCard("86%", "Attendance",
                        Color(0xFF0288D1), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(35.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Attendance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row {
                                ChartLegend(AbsentRed, "you")
                                Spacer(modifier = Modifier.width(16.dp))
                                ChartLegend(Color(0xFF42A5F5), "class avg")
                            }
                        }
                        Spacer(modifier = Modifier.height(35.dp))
                        AttendanceDualBarChart(
                            studentData = listOf(100f, 5f, 100f, 100f),
                            classAvgData = listOf(75f, 68f, 82f, 70f),
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        InfoRow("Enrollment Date", "April 11, 2026", Icons.Default.CalendarToday)
                        Spacer(modifier = Modifier.height(20.dp))
                        InfoRow("Assigned Class", "DSA (6C1)", Icons.Default.School)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoRowPreview() {
    AttendanceTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            InfoRow(
                label = "Enrollment Date",
                value = "April 11, 2026",
                icon = Icons.Default.CalendarToday
            )
        }
    }
}
