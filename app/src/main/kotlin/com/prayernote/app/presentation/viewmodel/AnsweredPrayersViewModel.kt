package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnsweredPrayersViewModel @Inject constructor(
    private val repository: PrayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnsweredPrayersUiState>(AnsweredPrayersUiState.Loading)
    val uiState: StateFlow<AnsweredPrayersUiState> = _uiState.asStateFlow()

    val answeredPrayers: StateFlow<List<PrayerTopic>> = repository.getAnsweredPrayers()
        .catch { exception ->
            _uiState.value = AnsweredPrayersUiState.Error(exception.message ?: "알 수 없는 오류")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val answeredPrayersPaged: Flow<PagingData<PrayerTopic>> = repository.getAnsweredPrayersPaged()
        .cachedIn(viewModelScope)

    init {
        loadAnsweredPrayers()
    }

    private fun loadAnsweredPrayers() {
        viewModelScope.launch {
            try {
                _uiState.value = AnsweredPrayersUiState.Loading
                answeredPrayers.collect { prayers ->
                    _uiState.value = if (prayers.isEmpty()) {
                        AnsweredPrayersUiState.Empty
                    } else {
                        AnsweredPrayersUiState.Success(prayers)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AnsweredPrayersUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun getGroupedByMonth(): Map<String, List<PrayerTopic>> {
        val prayers = answeredPrayers.value
        return prayers.groupBy { prayer ->
            prayer.answeredAt?.let {
                val calendar = java.util.Calendar.getInstance()
                calendar.time = it
                "${calendar.get(java.util.Calendar.YEAR)}년 ${calendar.get(java.util.Calendar.MONTH) + 1}월"
            } ?: "날짜 없음"
        }
    }
}

sealed class AnsweredPrayersUiState {
    object Loading : AnsweredPrayersUiState()
    object Empty : AnsweredPrayersUiState()
    data class Success(val prayers: List<PrayerTopic>) : AnsweredPrayersUiState()
    data class Error(val message: String) : AnsweredPrayersUiState()
}
