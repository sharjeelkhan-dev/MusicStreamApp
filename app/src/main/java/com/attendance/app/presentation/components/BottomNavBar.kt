package com.attendance.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.R
import com.attendance.app.presentation.navigation.Screen
import com.attendance.app.presentation.theme.*

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: Any,
    val unselectedIcon: Any
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", R.drawable.house_icon, R.drawable.house_icon),
    BottomNavItem(Screen.Classes, "Classes", R.drawable.classes, R.drawable.classes),
    BottomNavItem(Screen.TakeAttendance, "Attend", R.drawable.hand_line_icon, R.drawable.hand_line_icon),
    BottomNavItem(Screen.Reports, "Reports", R.drawable.reports_icon, R.drawable.reports_icon),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    NavigationBar(
        modifier = modifier,
        containerColor = if (isDark) Color.Black else Color.White,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            
            val iconPainter = when (val icon = if (isSelected) item.selectedIcon else item.unselectedIcon) {
                is ImageVector -> rememberVectorPainter(icon)
                is Int -> painterResource(id = icon)
                else -> throw IllegalArgumentException("Unsupported icon type")
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        painter = iconPainter,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    AttendanceTheme(darkTheme = false) {
        Surface {
            BottomNavBar(
                currentRoute = Screen.Home.route,
                onNavigate = {}
            )
        }
    }
}
