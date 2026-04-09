package com.attendance.app.presentation.students
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.presentation.components.StatsCard
import com.attendance.app.presentation.theme.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.StudentDetailScreen(
    studentName: String,
    studentRoll: String,
    initials: String,
    avatarColor: Color,
    animatedVisibilityScope: AnimatedContentScope,
    onBack: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Header Section - Maintained as requested (PrimaryGreenDark background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryGreenDark)
                    .statusBarsPadding()
                    .height(100.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(end = 5.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp).offset(x = (-5).dp, y = 10.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(top = 32.dp))
                    {
                        Text(
                            text = "Student Details",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = studentRoll,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Section
            Spacer(modifier = Modifier.height(40.dp))
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = avatarColor,
                    modifier = Modifier
                        .size(170.dp)
                        .shadow(20.dp, CircleShape, spotColor = avatarColor.copy(alpha = 0.5f))
                        .sharedElement(
                            rememberSharedContentState(key = "avatar_$studentRoll"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Verified Badge
                Surface(
                    modifier = Modifier.size(46.dp).offset(x = (-4).dp, y = (-4).dp),
                    shape = CircleShape,
                    color = PresentGreen,
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                    shadowElevation = 8.dp
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name
            Text(
                text = studentName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "name_$studentRoll"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Roll Tag (Light Green Pill)
            Surface(
                color = (if (isDark) Color.Transparent else Color.Transparent),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.offset(y = (-12).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = studentRoll,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) Color.White else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    label = "Present",
                    value = "18",
                    valueColor = PresentGreen,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    label = "Absent",
                    value = "02",
                    valueColor = AbsentRed,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    label = "Attendance",
                    value = "90%",
                    valueColor = Color(0xFF00ACC1),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Attendance Log Card (Refined to match reference image)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {

                        Column {
                            Text(
                                text = "Logs",
                                modifier = Modifier.offset(x = 5.dp, y = 5.dp,),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                lineHeight = 22.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LogTab("All", isSelected = true)
                            LogTab("Present", isSelected = false)
                            LogTab("Absent", isSelected = false)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    AttendanceLogItem("Thu", "03", "Apr 03", "Lecture 20 . Data Structures", true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    AttendanceLogItem("Wed", "02", "Apr 02", "Lecture 19 . Data Structures", true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    AttendanceLogItem("Tue", "01", "Apr 01", "Lecture 18 . Data Structures", false)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    AttendanceLogItem("Mon", "31", "Mar 31", "Lecture 17 . Data Structures", true)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Info Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Enrollment Date",
                        value = "January 15, 2024"
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    DetailRow(
                        icon = Icons.Rounded.School,
                        label = "Assigned Class",
                        value = "Software Engineering (6C1)"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LogTab(text: String, isSelected: Boolean) {
    Surface(
        color = if (isSelected) PrimaryGreen else Color.LightGray,
        shape = RoundedCornerShape(28.dp),
        border = if (isSelected) null else BorderStroke(1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.White else Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AttendanceLogItem(
    day: String,
    date: String,
    monthDate: String,
    lectureInfo: String,
    isPresent: Boolean
) {
    val statusColor = if (isPresent) PresentGreen else AbsentRed
    val bgColor = statusColor.copy(alpha = 0.1f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Box
        Column(
            modifier = Modifier
                .size(width = 54.dp, height = 50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.5.dp,
                    statusColor.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = date,
                style = MaterialTheme.typography.titleLarge,
                color = statusColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
        }
        
        Spacer(modifier = Modifier.width(18.dp))
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = monthDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
            Text(
                text = lectureInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
        
        // Status Pill
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (isPresent) "Present" else "Absent",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    val isDark = LocalIsDarkMode.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(14.dp),
            color = (if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)).copy(alpha = 0.6f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) Color.White else Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun StudentDetailPreview() {
    AttendanceTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true, label = "student_detail_preview") {
                StudentDetailScreen(
                    studentName = "John Doe",
                    studentRoll = "CS-01",
                    initials = "JD",
                    avatarColor = PrimaryGreen,
                    animatedVisibilityScope = this,
                    onBack = {}
                )
            }
        }
    }
}
