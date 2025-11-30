package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.data.local.entity.PrayerTopicWithPerson
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

    private val _uiEvent = MutableSharedFlow<AnsweredPrayersEvent>()
    val uiEvent: SharedFlow<AnsweredPrayersEvent> = _uiEvent.asSharedFlow()

    val answeredPrayers: StateFlow<List<PrayerTopic>> = repository.getAnsweredPrayers()
        .catch { exception ->
            _uiState.value = AnsweredPrayersUiState.Error(exception.message ?: "알 수 없는 오류")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val answeredPrayersWithPerson: StateFlow<List<PrayerTopicWithPerson>> = repository.getAnsweredPrayersWithPerson()
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
                answeredPrayersWithPerson.collect { prayers ->
                    _uiState.value = if (prayers.isEmpty()) {
                        AnsweredPrayersUiState.Empty
                    } else {
                        AnsweredPrayersUiState.Success(prayers.map { it.prayerTopic })
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AnsweredPrayersUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun getGroupedByMonth(): Map<String, List<PrayerTopicWithPerson>> {
        val prayers = answeredPrayersWithPerson.value
        return prayers.groupBy { prayerWithPerson ->
            prayerWithPerson.prayerTopic.answeredAt?.let {
                val calendar = java.util.Calendar.getInstance()
                calendar.time = it
                "${calendar.get(java.util.Calendar.YEAR)}년 ${calendar.get(java.util.Calendar.MONTH) + 1}월"
            } ?: "날짜 없음"
        }
    }

    fun deletePrayerTopic(topic: PrayerTopic) {
        viewModelScope.launch {
            try {
                repository.deletePrayerTopic(topic)
                _uiEvent.emit(AnsweredPrayersEvent.TopicDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(AnsweredPrayersEvent.Error(e.message ?: "삭제 실패"))
            }
        }
    }

    fun restoreTopic(topic: PrayerTopic) {
        viewModelScope.launch {
            try {
                val restoredTopic = topic.copy(
                    status = com.prayernote.app.data.local.entity.PrayerStatus.ACTIVE,
                    answeredAt = null
                )
                repository.updatePrayerTopic(restoredTopic)
                _uiEvent.emit(AnsweredPrayersEvent.TopicRestored)
            } catch (e: Exception) {
                _uiEvent.emit(AnsweredPrayersEvent.Error(e.message ?: "복원 실패"))
            }
        }
    }
}

sealed class AnsweredPrayersUiState {
    object Loading : AnsweredPrayersUiState()
    object Empty : AnsweredPrayersUiState()
    data class Success(val prayers: List<PrayerTopic>) : AnsweredPrayersUiState()
    data class Error(val message: String) : AnsweredPrayersUiState()
}

sealed class AnsweredPrayersEvent {
    object TopicDeleted : AnsweredPrayersEvent()
    object TopicRestored : AnsweredPrayersEvent()
    data class Error(val message: String) : AnsweredPrayersEvent()
}
