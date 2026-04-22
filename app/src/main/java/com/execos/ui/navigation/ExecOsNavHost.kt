package com.execos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.execos.ui.account.AccountScreen
import com.execos.ui.calendar.CalendarDayDetailsScreen
import com.execos.ui.calendar.CalendarScreen
import com.execos.ui.decisions.DecisionLogScreen
import com.execos.ui.energy.EnergyTrackerScreen
import com.execos.ui.focus.FocusPlannerScreen
import com.execos.ui.goals.GoalsScreen
import com.execos.ui.home.HomeScreen
import com.execos.ui.reflection.ReflectionScreen
import com.execos.ui.usage.UsageScreen
import com.execos.ui.weekly.WeeklyReviewScreen

@Composable
fun ExecOsNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ExecNavigationBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onNavigate = { route ->
                        if (route == Routes.Energy) {
                            navController.navigate(Routes.Energy) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
            composable(Routes.Focus) {
                FocusPlannerScreen()
            }
            composable(Routes.Decisions) {
                DecisionLogScreen()
            }
            composable(Routes.Reflection) {
                ReflectionScreen()
            }
            composable(Routes.Weekly) {
                WeeklyReviewScreen()
            }
            composable(Routes.Energy) {
                EnergyTrackerScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Account) {
                AccountScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Goals) {
                GoalsScreen()
            }
            composable(Routes.Usage) {
                UsageScreen()
            }
            composable(Routes.Calendar) {
                CalendarScreen(
                    onOpenDateDetails = { dateIso ->
                        navController.navigate(Routes.calendarDayDetails(dateIso)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.CalendarDayDetails,
                arguments = listOf(navArgument("dateIso") { type = NavType.StringType }),
            ) {
                CalendarDayDetailsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
