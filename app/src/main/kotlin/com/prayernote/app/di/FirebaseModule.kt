package com.prayernote.app.di

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    private fun ensureFirebaseInitialized(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                Log.d("FirebaseModule", "No Firebase apps found, initializing...")
                FirebaseApp.initializeApp(context)
                Log.d("FirebaseModule", "Firebase app initialized from module")
            } else {
                Log.d("FirebaseModule", "Firebase already initialized, ${apps.size} app(s) found")
            }
        } catch (e: Exception) {
            Log.e("FirebaseModule", "Failed to ensure Firebase initialization: ${e.message}", e)
            e.printStackTrace()
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth? {
        return try {
            ensureFirebaseInitialized(context)
            
            val auth = FirebaseAuth.getInstance()
            Log.d("FirebaseModule", "Firebase Auth obtained successfully")
            auth
        } catch (e: Exception) {
            Log.e("FirebaseModule", "Failed to obtain Firebase Auth: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore? {
        return try {
            ensureFirebaseInitialized(context)
            
            val firestore = FirebaseFirestore.getInstance()
            Log.d("FirebaseModule", "Firebase Firestore obtained successfully")
            firestore
        } catch (e: Exception) {
            Log.e("FirebaseModule", "Failed to obtain Firebase Firestore: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}
