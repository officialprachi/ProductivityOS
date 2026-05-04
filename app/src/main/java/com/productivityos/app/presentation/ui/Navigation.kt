package com.productivityos.app.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.productivityos.app.R
import com.productivityos.app.presentation.ui.screens.*
import com.productivityos.app.presentation.ui.theme.*

// ── Route constants ───────────────────────────────────────────

object Routes {
    const val HOME      = "home"
    const val INSIGHTS  = "insights"
    const val ANALYTICS = "analytics"
    const val FOCUS     = "focus"
    const val SCHEDULE  = "schedule"
    const val PROFILE   = "profile"
    const val SETTINGS  = "settings"
}

// ── Bottom nav items (4 visible + 1 FAB center) ───────────────

sealed class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit
) {
    object Home : BottomNavItem(
        Routes.HOME, R.string.nav_home,
        icon = {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconMd)
            )
        }
    )
    object Stats : BottomNavItem(
        Routes.ANALYTICS, R.string.nav_stats,
        icon = {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconMd)
            )
        }
    )
    object Focus : BottomNavItem(
        Routes.FOCUS, R.string.nav_focus,
        icon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconMd)
            )
        }
    )
    object Me : BottomNavItem(
        Routes.PROFILE, R.string.nav_me,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconMd)
            )
        }
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Stats,
    BottomNavItem.Focus,
    BottomNavItem.Me
)

// ── NavHost ───────────────────────────────────────────────────

@Composable
fun ProductivityNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(200)) }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateFocus = { navController.navigate(Routes.FOCUS) },
                onNavigateInsights = { navController.navigate(Routes.INSIGHTS) }
            )
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen()
        }
        composable(Routes.ANALYTICS) {
            AnalyticsScreen(
                onNavigateFocus = {
                    navController.navigate(Routes.FOCUS) {
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }
        composable(Routes.FOCUS) {
            FocusScreen()
        }
        composable(Routes.SCHEDULE) {
            ScheduleScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}

// ── Bottom Nav Bar ────────────────────────────────────────────

@Composable
fun ProductivityBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on settings & schedule screens
    val hideOnRoutes = setOf(Routes.SETTINGS, Routes.SCHEDULE, Routes.INSIGHTS)
    val currentRoute = currentDestination?.route
    if (currentRoute in hideOnRoutes) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink2)
            .border(
                width = 1.dp,
                color = Ink3,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            BottomNavTab(
                item = BottomNavItem.Home,
                isSelected = currentDestination?.hierarchy?.any { it.route == Routes.HOME } == true,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateToTab(Routes.HOME) }
            )

            // Stats
            BottomNavTab(
                item = BottomNavItem.Stats,
                isSelected = currentDestination?.hierarchy?.any { it.route == Routes.ANALYTICS } == true,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateToTab(Routes.ANALYTICS) }
            )

            // FAB — center
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Plum)
                        .clickable { navController.navigate(Routes.SCHEDULE) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", style = MaterialTheme.typography.headlineMedium.copy(color = Ink))
                }
            }

            // Focus
            BottomNavTab(
                item = BottomNavItem.Focus,
                isSelected = currentDestination?.hierarchy?.any { it.route == Routes.FOCUS } == true,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateToTab(Routes.FOCUS) }
            )

            // Me
            BottomNavTab(
                item = BottomNavItem.Me,
                isSelected = currentDestination?.hierarchy?.any { it.route == Routes.PROFILE } == true,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateToTab(Routes.PROFILE) }
            )
        }
    }
}

@Composable
private fun BottomNavTab(
    item: BottomNavItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (isSelected) Plum else White.copy(alpha = 0.25f)
        ) {
            item.icon()
        }
        Text(
            text = stringResource(item.labelRes).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) Plum else White.copy(alpha = 0.25f),
                fontSize = 7.sp
            ),
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
