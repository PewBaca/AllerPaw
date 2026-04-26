package com.allerpaw.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.allerpaw.app.ui.auth.LoginScreen
import com.allerpaw.app.ui.auth.LoginViewModel
import com.allerpaw.app.ui.export.ExportScreen
import com.allerpaw.app.ui.nav.BottomNavItem
import com.allerpaw.app.ui.nav.Screen
import com.allerpaw.app.ui.rezept.RezeptScreen
import com.allerpaw.app.ui.settings.SettingsScreen
import com.allerpaw.app.ui.stammdaten.StammdatenScreen
import com.allerpaw.app.ui.statistik.StatistikScreen
import com.allerpaw.app.ui.tagebuch.TagebuchScreen
import com.allerpaw.app.ui.zutaten.ZutatenScreen

@Composable
fun AllerPawApp() {
    val loginViewModel: LoginViewModel = hiltViewModel()

    // isLoggedIn startet als null (unbekannt) → zeigt Lade-Spinner
    // verhindert kurzen schwarzen Screen oder falschen Login-Flash
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
    val isLoading  by loginViewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController   = rememberNavController()
    val startDestination = if (isLoggedIn) Screen.Tagebuch.route else Screen.Login.route
    val bottomNavItems  = BottomNavItem.entries

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon    = { Icon(item.icon(), stringResource(item.labelRes)) },
                            label   = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(viewModel = loginViewModel, onLoginSuccess = {
                    navController.navigate(Screen.Tagebuch.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Tagebuch.route) {
                TagebuchScreen(onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                })
            }
            // Rechner-Tab zeigt jetzt den vollen RezeptScreen
            composable(Screen.Rechner.route) { RezeptScreen() }
            composable(Screen.Stammdaten.route) {
                StammdatenScreen(onNavigateToZutaten = {
                    navController.navigate(Screen.Zutaten.route)
                })
            }
            composable(Screen.Zutaten.route)   { ZutatenScreen() }
            composable(Screen.Statistik.route) { StatistikScreen() }
            composable(Screen.Export.route)    { ExportScreen() }
            composable(Screen.Settings.route)  {
                SettingsScreen(onNavigateUp = { navController.navigateUp() })
            }
        }
    }
}

private fun BottomNavItem.icon() = when (this) {
    BottomNavItem.TAGEBUCH   -> Icons.Default.Book
    BottomNavItem.RECHNER    -> Icons.Default.Calculate
    BottomNavItem.STAMMDATEN -> Icons.Default.Pets
    BottomNavItem.STATISTIK  -> Icons.Default.BarChart
    BottomNavItem.EXPORT     -> Icons.Default.PictureAsPdf
}
