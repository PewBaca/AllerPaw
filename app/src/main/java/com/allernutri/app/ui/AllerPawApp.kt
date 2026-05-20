package com.allernutri.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.window.core.layout.WindowWidthSizeClass
import com.allernutri.app.ui.auth.LoginScreen
import com.allernutri.app.ui.auth.LoginViewModel
import com.allernutri.app.ui.export.ExportScreen
import com.allernutri.app.ui.nav.BottomNavItem
import com.allernutri.app.ui.nav.Screen
import com.allernutri.app.ui.rezept.RezeptScreen
import com.allernutri.app.ui.settings.BackupScreen
import com.allernutri.app.ui.settings.SettingsScreen
import com.allernutri.app.ui.sheets.SheetsImportScreen
import com.allernutri.app.ui.sheets.SheetsExportScreen
import com.allernutri.app.ui.tagebuch.KreuzallergenScreen
import com.allernutri.app.ui.zutaten.ZutatVergleichScreen
import com.allernutri.app.ui.zutaten.FoodImportScreen
import com.allernutri.app.ui.settings.ApiKeysScreen
import com.allernutri.app.ui.stammdaten.StammdatenScreen
import com.allernutri.app.ui.statistik.StatistikScreen
import com.allernutri.app.ui.tagebuch.TagebuchScreen
import com.allernutri.app.ui.tasks.TaskScreen
import com.allernutri.app.ui.zutaten.ZutatenScreen

@Composable
fun AllerPawApp() {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
    val isLoading  by loginViewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController    = rememberNavController()
    val startDestination = if (isLoggedIn) Screen.Tagebuch.route else Screen.Login.route
    val bottomNavItems   = BottomNavItem.entries

    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showNav = bottomNavItems.any { it.screen.route == currentDestination?.route }

    // Adaptive: NavigationRail auf Medium/Expanded, NavigationBar auf Compact
    val windowInfo = currentWindowAdaptiveInfo()
    val useRail = windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val navContent: @Composable () -> Unit = {
        NavHost(
            navController    = navController,
            startDestination = startDestination,
        ) {
            composable(Screen.Login.route) {
                LoginScreen(viewModel = loginViewModel, onLoginSuccess = {
                    navController.navigate(Screen.Tagebuch.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Tagebuch.route) {
                TagebuchScreen(
                    onNavigateToSettings      = { navController.navigate(Screen.Settings.route) },
                    onNavigateToKreuzallergen = { navController.navigate(Screen.Kreuzallergen.route) }
                )
            }
            composable(Screen.Rechner.route)    { RezeptScreen() }
            composable(Screen.Stammdaten.route) {
                StammdatenScreen(onNavigateToZutaten = {
                    navController.navigate(Screen.Zutaten.route)
                })
            }
            composable(Screen.Zutaten.route) {
                ZutatenScreen(
                    onNavigateToVergleich = { navController.navigate(Screen.ZutatVergleich.route) },
                    onNavigateToImport    = { navController.navigate(Screen.FoodImport.route) }
                )
            }
            composable(Screen.Statistik.route)  { StatistikScreen() }
            composable(Screen.Tasks.route)      { TaskScreen() }
            composable(Screen.Export.route)     { ExportScreen() }
            composable(Screen.Settings.route)   {
                SettingsScreen(
                    onNavigateUp           = { navController.navigateUp() },
                    onNavigateBackup       = { navController.navigate(Screen.Backup.route) },
                    onNavigateSheets       = { navController.navigate(Screen.SheetsImport.route) },
                    onNavigateSheetsExport = { navController.navigate(Screen.SheetsExport.route) },
                    onNavigateApiKeys      = { navController.navigate(Screen.ApiKeys.route) }
                )
            }
            composable(Screen.Backup.route) {
                BackupScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(Screen.SheetsImport.route) {
                SheetsImportScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(Screen.SheetsExport.route) {
                SheetsExportScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(Screen.Kreuzallergen.route) {
                KreuzallergenScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(Screen.ZutatVergleich.route) {
                ZutatVergleichScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(Screen.FoodImport.route) {
                FoodImportScreen(
                    onNavigateUp        = { navController.navigateUp() },
                    onNavigateToApiKeys = { navController.navigate(Screen.ApiKeys.route) }
                )
            }
            composable(Screen.ApiKeys.route) {
                ApiKeysScreen(onNavigateUp = { navController.navigateUp() })
            }
        }
    }

    if (showNav && useRail) {
        // ── Large screen: NavigationRail links ───────────────────────────
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                bottomNavItems.forEach { item ->
                    NavigationRailItem(
                        icon     = { Icon(item.icon(), stringResource(item.labelRes)) },
                        label    = { Text(stringResource(item.labelRes)) },
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == item.screen.route } == true,
                        onClick  = {
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
            Box(Modifier.weight(1f)) { navContent() }
        }
    } else {
        // ── Phone: NavigationBar unten ────────────────────────────────────
        Scaffold(
            bottomBar = {
                if (showNav) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon     = { Icon(item.icon(), stringResource(item.labelRes)) },
                                label    = { Text(stringResource(item.labelRes)) },
                                selected = currentDestination?.hierarchy
                                    ?.any { it.route == item.screen.route } == true,
                                onClick  = {
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
            Box(Modifier.padding(innerPadding)) { navContent() }
        }
    }
}

private fun BottomNavItem.icon() = when (this) {
    BottomNavItem.TAGEBUCH   -> Icons.Default.Book
    BottomNavItem.RECHNER    -> Icons.Default.Calculate
    BottomNavItem.STAMMDATEN -> Icons.Default.Pets
    BottomNavItem.STATISTIK  -> Icons.Default.BarChart
    BottomNavItem.TASKS      -> Icons.Default.Checklist
    BottomNavItem.EXPORT     -> Icons.Default.PictureAsPdf
}
