package com.allerpaw.app.ui.nav

sealed class Screen(val route: String) {
    object Login      : Screen("login")
    object Settings   : Screen("settings")
    object Stammdaten : Screen("stammdaten")
    object Zutaten    : Screen("zutaten")
    object Rechner    : Screen("rechner")
    object Tagebuch   : Screen("tagebuch")
    object Statistik  : Screen("statistik")
    object Tasks      : Screen("tasks")
    object Export     : Screen("export")
}

enum class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val iconName: String
) {
    TAGEBUCH  (Screen.Tagebuch,   com.allerpaw.app.R.string.nav_tagebuch,   "Book"),
    RECHNER   (Screen.Rechner,    com.allerpaw.app.R.string.nav_rechner,    "Calculate"),
    STAMMDATEN(Screen.Stammdaten, com.allerpaw.app.R.string.nav_stammdaten, "Pets"),
    STATISTIK (Screen.Statistik,  com.allerpaw.app.R.string.nav_statistik,  "BarChart"),
    TASKS     (Screen.Tasks,      com.allerpaw.app.R.string.nav_tasks,      "Checklist"),
    EXPORT    (Screen.Export,     com.allerpaw.app.R.string.nav_export,     "PictureAsPdf"),
}
