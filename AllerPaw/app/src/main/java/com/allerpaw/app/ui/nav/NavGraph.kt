package com.allerpaw.app.ui.nav

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Settings : Screen("settings")

    // Stammdaten
    object Stammdaten : Screen("stammdaten")

    // Rechner
    object Rechner : Screen("rechner")

    // Tagebuch
    object Tagebuch : Screen("tagebuch")

    // Statistik
    object Statistik : Screen("statistik")

    // Export
    object Export : Screen("export")
}

/** Bottom nav items shown after login */
enum class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val iconName: String // resolved in Composable via Icons.Default.*
) {
    TAGEBUCH(Screen.Tagebuch, com.allerpaw.app.R.string.nav_tagebuch, "Book"),
    RECHNER(Screen.Rechner, com.allerpaw.app.R.string.nav_rechner, "Calculate"),
    STAMMDATEN(Screen.Stammdaten, com.allerpaw.app.R.string.nav_stammdaten, "Pets"),
    STATISTIK(Screen.Statistik, com.allerpaw.app.R.string.nav_statistik, "BarChart"),
    EXPORT(Screen.Export, com.allerpaw.app.R.string.nav_export, "PictureAsPdf"),
}
