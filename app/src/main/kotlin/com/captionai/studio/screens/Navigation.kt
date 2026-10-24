package com.captionai.studio.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.captionai.studio.utils.BillingHelper

@Composable
fun CaptionAIApp(billingHelper: BillingHelper) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            Column {
                BannerAdView()
                FloatingBottomNavigationBar(navController = navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(
                route = Screen.Result.route + "?platform={platform}&content={content}&tone={tone}&length={length}&context={context}",
                arguments = listOf(
                    navArgument("platform") { defaultValue = "INSTAGRAM" },
                    navArgument("content") { defaultValue = "Selfie" },
                    navArgument("tone") { defaultValue = "Cool" },
                    navArgument("length") { defaultValue = "Standard" },
                    navArgument("context") { defaultValue = "" }
                )
            ) { backStackEntry ->
                ResultScreen(
                    navController = navController,
                    platform = backStackEntry.arguments?.getString("platform") ?: "INSTAGRAM",
                    content = Uri.decode(backStackEntry.arguments?.getString("content") ?: "Selfie"),
                    tone = Uri.decode(backStackEntry.arguments?.getString("tone") ?: "Cool"),
                    length = Uri.decode(backStackEntry.arguments?.getString("length") ?: "Standard"),
                    contextArg = Uri.decode(backStackEntry.arguments?.getString("context") ?: "")
                )
            }
            composable(Screen.Trending.route) { TrendingScreen(navController) }
            composable(Screen.Favorites.route) { FavoritesScreen(navController) }
            composable(Screen.History.route) { HistoryScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController, billingHelper) }
        }
    }
}

data class NavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

@Composable
fun FloatingBottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavItem(Screen.Home.route, "Write", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        NavItem(Screen.Trending.route, "Trends", Icons.Filled.Whatshot, Icons.Outlined.Whatshot),
        NavItem(Screen.Favorites.route, "Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        NavItem(Screen.History.route, "History", Icons.Filled.History, Icons.Outlined.History),
        NavItem(Screen.Settings.route, "Setup", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentDestination?.route != item.route) {
                                if (item.route == Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val iconColor = if (isSelected) Color(0xFF7D5FFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        val fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                        
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            color = iconColor,
                            fontWeight = fontWeight
                        )
                    }
                }
            }
        }
    }
}
