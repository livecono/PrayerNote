package com.prayernote.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PersonList : Screen("person_list")
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: Long) = "person_detail/$personId"
    }
    object PersonSelection : Screen("person_selection")
    object CameraOCR : Screen("camera_ocr/{personId}") {
        fun createRoute(personId: Long) = "camera_ocr/$personId"
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
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.Home.route,
        titleResId = com.prayernote.app.R.string.nav_home,
        icon = Icons.Filled.Home
    )

    object PersonList : BottomNavItem(
        route = Screen.PersonList.route,
        titleResId = com.prayernote.app.R.string.nav_person_list,
        icon = Icons.Filled.People
    )

    object DayAssignment : BottomNavItem(
        route = Screen.DayAssignment.route,
        titleResId = com.prayernote.app.R.string.nav_day_assignment,
        icon = Icons.Filled.DateRange
    )

    object Answered : BottomNavItem(
        route = Screen.AnsweredPrayers.route,
        titleResId = com.prayernote.app.R.string.nav_answered,
        icon = Icons.Filled.CheckCircle
    )

    object Statistics : BottomNavItem(
        route = Screen.Statistics.route,
        titleResId = com.prayernote.app.R.string.nav_statistics,
        icon = Icons.Filled.BarChart
    )

    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        titleResId = com.prayernote.app.R.string.nav_settings,
        icon = Icons.Filled.Settings
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.PersonList,
    BottomNavItem.DayAssignment,
    BottomNavItem.Answered,
    BottomNavItem.Statistics,
    BottomNavItem.Settings
)
