package com.allernutri.app.ui.nav

sealed class Screen(val route: String) {
    object Login        : Screen("login")
    object Settings     : Screen("settings")
    object Backup       : Screen("backup")
    object SheetsImport  : Screen("sheets_import")
    object SheetsExport  : Screen("sheets_export")
    object Kreuzallergen  : Screen("kreuzallergen")
    object ZutatVergleich : Screen("zutat_vergleich")
    object FoodImport     : Screen("food_import")
    object ApiKeys        : Screen("api_keys")
    object Stammdaten   : Screen("stammdaten")
    object Zutaten      : Screen("zutaten")
    object Rechner      : Screen("rechner")
    object Tagebuch     : Screen("tagebuch")
    object Statistik    : Screen("statistik")
    object Tasks        : Screen("tasks")
    object Export       : Screen("export")
}

enum class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val iconName: String
) {
    TAGEBUCH  (Screen.Tagebuch,   com.allernutri.app.R.string.nav_tagebuch,   "Book"),
    RECHNER   (Screen.Rechner,    com.allernutri.app.R.string.nav_rechner,    "Calculate"),
    STAMMDATEN(Screen.Stammdaten, com.allernutri.app.R.string.nav_stammdaten, "Pets"),
    STATISTIK (Screen.Statistik,  com.allernutri.app.R.string.nav_statistik,  "BarChart"),
    TASKS     (Screen.Tasks,      com.allernutri.app.R.string.nav_tasks,      "Checklist"),
    EXPORT    (Screen.Export,     com.allernutri.app.R.string.nav_export,     "PictureAsPdf"),
}
