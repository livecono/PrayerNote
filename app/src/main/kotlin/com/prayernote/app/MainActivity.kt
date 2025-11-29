package com.prayernote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.prayernote.app.data.datastore.PreferencesDataStore
import com.prayernote.app.presentation.components.PrayerNoteBottomBar
import com.prayernote.app.presentation.navigation.PrayerNoteNavHost
import com.prayernote.app.presentation.navigation.Screen
import com.prayernote.app.presentation.viewmodel.ThemeMode
import com.prayernote.app.ui.theme.PrayerNoteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isFirstLaunch by preferencesDataStore.isFirstLaunch.collectAsState(initial = true)

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            PrayerNoteTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val startDestination = if (isFirstLaunch) {
                    Screen.Onboarding.route
                } else {
                    Screen.PersonList.route
                }

                Scaffold(
                    bottomBar = {
                        // Only show bottom bar if not on onboarding
                        if (!isFirstLaunch) {
                            PrayerNoteBottomBar(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    PrayerNoteNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        preferencesDataStore = preferencesDataStore,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
