package com.prayernote.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.prayernote.app.data.datastore.PreferencesDataStore
import com.prayernote.app.presentation.screen.*

@Composable
fun PrayerNoteNavHost(
    navController: NavHostController,
    startDestination: String,
    preferencesDataStore: PreferencesDataStore,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                preferencesDataStore = preferencesDataStore,
                onComplete = {
                    navController.navigate(Screen.PersonList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PersonList.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "prayernote://home?dayOfWeek={dayOfWeek}"
                }
            )
        ) {
            PersonListScreen(
                onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetail.createRoute(personId))
                }
            )
        }

        composable(
            route = Screen.PersonDetail.route,
            arguments = listOf(
                navArgument("personId") {
                    type = NavType.LongType
                }
            )
        ) {
            PersonDetailScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.DayAssignment.route) {
            DayAssignmentScreen()
        }

        composable(Screen.AnsweredPrayers.route) {
            AnsweredPrayersScreen()
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
