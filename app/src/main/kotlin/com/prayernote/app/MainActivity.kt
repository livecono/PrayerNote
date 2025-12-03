package com.prayernote.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.prayernote.app.presentation.viewmodel.PersonListViewModel
import com.prayernote.app.presentation.viewmodel.SharedViewModel
import com.prayernote.app.presentation.viewmodel.ThemeMode
import com.prayernote.app.ui.theme.PrayerNoteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    private val sharedViewModel: SharedViewModel by viewModels()
    private val personListViewModel: PersonListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle shared text
        handleSharedIntent(intent)
        
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
                    Screen.Home.route
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
                        sharedViewModel = sharedViewModel,
                        personListViewModel = personListViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // 새로운 Intent를 현재 Intent로 설정
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        Log.d(TAG, "handleSharedIntent called")
        Log.d(TAG, "Intent action: ${intent?.action}")
        Log.d(TAG, "Intent type: ${intent?.type}")
        
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    Log.d(TAG, "Shared text extracted: $text")
                    if (!text.isNullOrBlank()) {
                        Log.d(TAG, "Calling sharedViewModel.setSharedText")
                        sharedViewModel.setSharedText(text)
                    }
                }
            }
        }
    }
}
