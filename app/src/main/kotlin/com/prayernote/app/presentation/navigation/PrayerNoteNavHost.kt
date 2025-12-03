package com.prayernote.app.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.prayernote.app.data.datastore.PreferencesDataStore
import com.prayernote.app.presentation.components.PersonSelectionDialog
import com.prayernote.app.presentation.screen.*
import com.prayernote.app.presentation.viewmodel.PersonListViewModel
import com.prayernote.app.presentation.viewmodel.SharedViewModel

private const val TAG = "PrayerNoteNavHost"

@Composable
fun PrayerNoteNavHost(
    navController: NavHostController,
    startDestination: String,
    preferencesDataStore: PreferencesDataStore,
    sharedViewModel: SharedViewModel,
    personListViewModel: PersonListViewModel,
    modifier: Modifier = Modifier
) {
    val sharedText by sharedViewModel.sharedText.collectAsState()
    val showPersonSelectionDialog by sharedViewModel.showPersonSelectionDialog.collectAsState()
    
    Log.d(TAG, "NavHost recomposing - sharedText: $sharedText, showDialog: $showPersonSelectionDialog")

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                preferencesDataStore = preferencesDataStore,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Home.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "prayernote://home?dayOfWeek={dayOfWeek}"
                }
            )
        ) {
            HomeScreen(
                onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetail.createRoute(personId))
                }
            )
        }

        composable(Screen.PersonList.route) {
            PersonListScreen(
                onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetail.createRoute(personId))
                }
            )
        }

        composable(Screen.PersonSelection.route) {
            PersonSelectionScreen(
                sharedText = sharedText,
                onPersonSelected = { personId ->
                    navController.navigate(Screen.PersonDetail.createRoute(personId)) {
                        popUpTo(Screen.PersonSelection.route) { inclusive = true }
                    }
                },
                onCancel = {
                    sharedViewModel.dismissDialog()
                    navController.navigateUp()
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

    // Show person selection dialog when there's shared text
    val currentSharedText = sharedText
    if (showPersonSelectionDialog && !currentSharedText.isNullOrBlank()) {
        Log.d(TAG, "Showing PersonSelectionDialog")
        
        val persons by personListViewModel.persons.collectAsState()
        
        PersonSelectionDialog(
            sharedText = currentSharedText,
            persons = persons,
            onDismiss = { 
                Log.d(TAG, "Dialog dismissed")
                sharedViewModel.dismissDialog() 
            },
            onAddPrayerTopic = { personId, topic ->
                Log.d(TAG, "Adding topic to person: $personId")
                personListViewModel.addPrayerTopicToPerson(personId, topic)
                sharedViewModel.dismissDialog()
                navController.navigate(Screen.PersonDetail.createRoute(personId))
            },
            onAddPerson = { name, memo ->
                Log.d(TAG, "Adding new person: $name")
                personListViewModel.addPerson(name, memo)
            }
        )
    } else {
        Log.d(TAG, "Dialog NOT shown - showDialog: $showPersonSelectionDialog, sharedText: $currentSharedText")
    }
}
