package com.attendance.app.presentation.navigation
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendance.app.presentation.attendance.TakeAttendanceScreen
import com.attendance.app.presentation.classes.ClassesScreen
import com.attendance.app.presentation.components.BottomNavBar
import com.attendance.app.presentation.home.HomeScreen
import com.attendance.app.presentation.reports.ReportsScreen
import com.attendance.app.presentation.settings.SettingsScreen
import com.attendance.app.presentation.splash.SplashScreen
import com.attendance.app.presentation.students.StudentDetailScreen
import com.attendance.app.presentation.students.StudentsScreen
import androidx.activity.ComponentActivity
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar by remember(currentRoute) {
        derivedStateOf {
            currentRoute in listOf(
                Screen.Home.route,
                Screen.Classes.route,
                Screen.TakeAttendance.route,
                Screen.Reports.route,
                Screen.Students.route
            ) && currentRoute != Screen.Splash.route
        }
    }

    val transitionDuration = 700
    val customEasing = EaseOutQuart 

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(animationSpec = tween(transitionDuration)),
                exit = fadeOut(animationSpec = tween(transitionDuration))
            ) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    )
    { paddingValues ->
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(transitionDuration, easing = customEasing)
                    ) + fadeIn(animationSpec = tween(transitionDuration))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 }, // Parallax effect
                        animationSpec = tween(transitionDuration, easing = customEasing)
                    ) + fadeOut(animationSpec = tween(transitionDuration))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 }, // Parallax effect
                        animationSpec = tween(transitionDuration, easing = customEasing)
                    ) + fadeIn(animationSpec = tween(transitionDuration))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(transitionDuration, easing = customEasing)
                    ) + fadeOut(animationSpec = tween(transitionDuration))
                }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashComplete = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? ComponentActivity)?.reportFullyDrawn()
                    }
                    HomeScreen(
                        onNavigateToAttendance = {
                            navController.navigate(Screen.TakeAttendance.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToReports = {
                            navController.navigate(Screen.Reports.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToStudents = {
                            navController.navigate(Screen.Students.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToClasses = {
                            navController.navigate(Screen.Classes.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(bottom = if (showBottomBar) 0.dp else 0.dp),
                        paddingValues = paddingValues
                    )
                }

                composable(Screen.TakeAttendance.route) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? ComponentActivity)?.reportFullyDrawn()
                    }
                    TakeAttendanceScreen(
                        modifier = Modifier,
                        paddingValues = paddingValues
                    )
                }

                composable(Screen.Reports.route) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? ComponentActivity)?.reportFullyDrawn()
                    }
                    ReportsScreen(
                        modifier = Modifier,
                        paddingValues = paddingValues
                    )
                }

                composable(Screen.Students.route) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? ComponentActivity)?.reportFullyDrawn()
                    }
                    StudentsScreen(
                        onBack = { 
                            navController.popBackStack()
                        },
                        onStudentClick = { student, color ->
                            navController.navigate(
                                Screen.StudentDetail.createRoute(
                                    name = student.fullName,
                                    roll = student.rollNumber,
                                    initials = student.initials,
                                    color = color.value.toLong().toInt()
                                )
                            )
                        },
                        animatedVisibilityScope = this@composable,
                        modifier = Modifier,
                        paddingValues = paddingValues
                    )
                }

                composable(
                    route = Screen.StudentDetail.route,
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType },
                        navArgument("roll") { type = NavType.StringType },
                        navArgument("initials") { type = NavType.StringType },
                        navArgument("color") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val roll = backStackEntry.arguments?.getString("roll") ?: ""
                    val initials = backStackEntry.arguments?.getString("initials") ?: ""
                    val colorValue = backStackEntry.arguments?.getInt("color") ?: 0
                    
                    StudentDetailScreen(
                        studentName = name,
                        studentRoll = roll,
                        initials = initials,
                        avatarColor = Color(colorValue.toLong().toULong()),
                        animatedVisibilityScope = this@composable,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Classes.route) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? ComponentActivity)?.reportFullyDrawn()
                    }
                    ClassesScreen(
                        modifier = Modifier,
                        paddingValues = paddingValues
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { 
                            navController.popBackStack()
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
