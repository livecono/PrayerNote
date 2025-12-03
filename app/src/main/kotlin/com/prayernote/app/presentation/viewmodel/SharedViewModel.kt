package com.prayernote.app.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {
    
    companion object {
        private const val TAG = "SharedViewModel"
    }
    
    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText.asStateFlow()
    
    private val _showPersonSelectionDialog = MutableStateFlow(false)
    val showPersonSelectionDialog: StateFlow<Boolean> = _showPersonSelectionDialog.asStateFlow()
    
    fun setSharedText(text: String?) {
        Log.d(TAG, "setSharedText called with: $text")
        _sharedText.value = text
        if (text != null) {
            _showPersonSelectionDialog.value = true
            Log.d(TAG, "Dialog flag set to true")
        }
    }
    
    fun dismissDialog() {
        Log.d(TAG, "dismissDialog called")
        _showPersonSelectionDialog.value = false
        _sharedText.value = null
    }
}
