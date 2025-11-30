package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeEvent>()
    val uiEvent: SharedFlow<HomeEvent> = _uiEvent.asSharedFlow()

    private val _todayPrayers = MutableStateFlow<Map<Person, List<PrayerTopic>>>(emptyMap())
    val todayPrayers: StateFlow<Map<Person, List<PrayerTopic>>> = _todayPrayers.asStateFlow()

    init {
        loadTodayPrayers()
    }

    private fun loadTodayPrayers() {
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Loading
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

                repository.getPersonsByDay(dayOfWeek).collect { persons ->
                    if (persons.isEmpty()) {
                        _uiState.value = HomeUiState.Empty
                        _todayPrayers.value = emptyMap()
                    } else {
                        val prayersMap = mutableMapOf<Person, List<PrayerTopic>>()
                        
                        persons.forEach { personWithDay ->
                            val topics = repository.getPrayerTopicsByPersonAndStatus(
                                personWithDay.person.id,
                                PrayerStatus.ACTIVE
                            ).first()
                            
                            if (topics.isNotEmpty()) {
                                prayersMap[personWithDay.person] = topics
                            }
                        }

                        if (prayersMap.isEmpty()) {
                            _uiState.value = HomeUiState.Empty
                            _todayPrayers.value = emptyMap()
                        } else {
                            _uiState.value = HomeUiState.Success
                            _todayPrayers.value = prayersMap
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun markAsAnswered(topic: PrayerTopic) {
        viewModelScope.launch {
            try {
                val updatedTopic = topic.copy(
                    status = PrayerStatus.ANSWERED,
                    answeredAt = Date()
                )
                repository.updatePrayerTopic(updatedTopic)
                _uiEvent.emit(HomeEvent.TopicAnswered)
            } catch (e: Exception) {
                _uiEvent.emit(HomeEvent.Error(e.message ?: "완료 처리 실패"))
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty : HomeUiState()
    object Success : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class HomeEvent {
    object TopicAnswered : HomeEvent()
    data class Error(val message: String) : HomeEvent()
}
