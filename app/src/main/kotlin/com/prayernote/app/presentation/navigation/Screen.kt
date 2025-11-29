package com.prayernote.app.presentation.navigation

sealed class Screen(val route: String) {
    object PersonList : Screen("person_list")
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: Long) = "person_detail/$personId"
    }
    object DayAssignment : Screen("day_assignment")
    object AnsweredPrayers : Screen("answered_prayers")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
}

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.PersonList.route,
        titleResId = com.prayernote.app.R.string.nav_home,
        icon = androidx.compose.material.icons.Icons.Filled.Home
    )
    
    object DayAssignment : BottomNavItem(
        route = Screen.DayAssignment.route,
        titleResId = com.prayernote.app.R.string.nav_day_assignment,
        icon = androidx.compose.material.icons.Icons.Filled.DateRange
    )
    
    object Answered : BottomNavItem(
        route = Screen.AnsweredPrayers.route,
        titleResId = com.prayernote.app.R.string.nav_answered,
        icon = androidx.compose.material.icons.Icons.Filled.CheckCircle
    )
    
    object Statistics : BottomNavItem(
        route = Screen.Statistics.route,
        titleResId = com.prayernote.app.R.string.nav_statistics,
        icon = androidx.compose.material.icons.Icons.Filled.BarChart
    )
    
    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        titleResId = com.prayernote.app.R.string.nav_settings,
        icon = androidx.compose.material.icons.Icons.Filled.Settings
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.DayAssignment,
    BottomNavItem.Answered,
    BottomNavItem.Statistics,
    BottomNavItem.Settings
)
