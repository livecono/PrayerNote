package com.prayernote.app.presentation.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TextRecognitionViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val TAG = "TextRecognitionVM"
    }

    private val _recognitionState = MutableStateFlow<TextRecognitionState>(TextRecognitionState.Idle)
    val recognitionState: StateFlow<TextRecognitionState> = _recognitionState.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun recognizeTextFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _recognitionState.value = TextRecognitionState.Processing
                Log.d(TAG, "Starting text recognition from bitmap")

                val image = InputImage.fromBitmap(bitmap, 0)
                val result = textRecognizer.process(image).await()

                val recognizedText = result.text
                Log.d(TAG, "Recognized text: $recognizedText")

                if (recognizedText.isNotBlank()) {
                    _recognitionState.value = TextRecognitionState.Success(recognizedText)
                } else {
                    _recognitionState.value = TextRecognitionState.Error("텍스트를 찾을 수 없습니다")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Text recognition failed", e)
                _recognitionState.value = TextRecognitionState.Error(
                    e.message ?: "텍스트 인식에 실패했습니다"
                )
            }
        }
    }

    fun recognizeTextFromUri(uri: Uri, getBitmap: suspend (Uri) -> Bitmap?) {
        viewModelScope.launch {
            try {
                _recognitionState.value = TextRecognitionState.Processing
                Log.d(TAG, "Starting text recognition from URI: $uri")

                val bitmap = getBitmap(uri)
                if (bitmap != null) {
                    recognizeTextFromBitmap(bitmap)
                } else {
                    _recognitionState.value = TextRecognitionState.Error("이미지를 불러올 수 없습니다")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from URI", e)
                _recognitionState.value = TextRecognitionState.Error(
                    e.message ?: "이미지를 불러올 수 없습니다"
                )
            }
        }
    }

    fun resetState() {
        _recognitionState.value = TextRecognitionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}

sealed class TextRecognitionState {
    object Idle : TextRecognitionState()
    object Processing : TextRecognitionState()
    data class Success(val text: String) : TextRecognitionState()
    data class Error(val message: String) : TextRecognitionState()
}
