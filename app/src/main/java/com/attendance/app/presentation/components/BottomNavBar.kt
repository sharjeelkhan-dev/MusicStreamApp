package com.attendance.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.Class
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
import com.attendance.app.presentation.theme.AttendanceTheme
import com.attendance.app.presentation.theme.BottomNavSelected
import com.attendance.app.presentation.theme.BottomNavUnselected

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: Any,
    val unselectedIcon: Any
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", R.drawable.house_icon, R.drawable.house_icon),
    BottomNavItem(Screen.Classes, "Classes", Icons.Filled.Class, Icons.Outlined.Class),
    BottomNavItem(Screen.TakeAttendance, "Attend", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    BottomNavItem(Screen.Reports, "Reports", R.drawable.reports_icon, R.drawable.reports_icon),
    BottomNavItem(Screen.Students, "Students", R.drawable.graduation_cap_icon, R.drawable.graduation_cap_icon),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 2.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = BottomNavSelected,
        tonalElevation = 0.dp
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
                    selectedIconColor = BottomNavSelected,
                    selectedTextColor = BottomNavSelected,
                    unselectedIconColor = BottomNavUnselected,
                    unselectedTextColor = BottomNavUnselected,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    AttendanceTheme {
        Surface {
            BottomNavBar(
                currentRoute = Screen.Home.route,
                onNavigate = {}
            )
        }
    }
}
