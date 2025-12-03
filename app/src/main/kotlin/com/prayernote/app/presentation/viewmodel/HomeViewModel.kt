package com.prayernote.app.presentation.viewmodel

import android.util.Log
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
                Log.d("HomeViewModel", "loadTodayPrayers: dayOfWeek=$dayOfWeek")

                repository.getPersonsByDayOfWeek(dayOfWeek)
                    .flatMapLatest { persons ->
                        Log.d("HomeViewModel", "Persons for today: ${persons.size}")
                        if (persons.isEmpty()) {
                            flowOf(emptyMap<Person, List<PrayerTopic>>())
                        } else {
                            // 각 Person의 기도제목 Flow를 결합
                            combine(
                                persons.map { person ->
                                    repository.getPrayerTopicsByPersonAndStatus(
                                        person.id,
                                        PrayerStatus.ACTIVE
                                    ).map { topics ->
                                        Log.d("HomeViewModel", "Person ${person.name} has ${topics.size} topics")
                                        person to topics
                                    }
                                }
                            ) { topicsArray ->
                                topicsArray.toMap()
                            }
                        }
                    }
                    .collect { prayersMap ->
                        Log.d("HomeViewModel", "Total persons: ${prayersMap.size}")
                        if (prayersMap.isEmpty()) {
                            _uiState.value = HomeUiState.Empty
                            _todayPrayers.value = emptyMap()
                        } else {
                            _uiState.value = HomeUiState.Success
                            _todayPrayers.value = prayersMap
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading prayers", e)
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

    fun togglePersonForToday(personId: Long) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
                val person = repository.getPersonById(personId).first() ?: return@launch
                
                val updatedDays = person.dayOfWeekAssignment.toMutableSet()
                if (updatedDays.contains(dayOfWeek)) {
                    updatedDays.remove(dayOfWeek)
                } else {
                    updatedDays.add(dayOfWeek)
                }
                
                repository.updatePerson(person.copy(dayOfWeekAssignment = updatedDays))
                _uiEvent.emit(HomeEvent.PersonToggled)
            } catch (e: Exception) {
                _uiEvent.emit(HomeEvent.Error(e.message ?: "대상자 설정 실패"))
            }
        }
    }

    fun updateTodayPersons(selectedPersonIds: Set<Long>) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
                
                // Get all persons
                val allPersons = repository.getAllPersons().first()
                
                // Update each person's day assignment
                allPersons.forEach { person ->
                    val updatedDays = person.dayOfWeekAssignment.toMutableSet()
                    val shouldBeAssigned = selectedPersonIds.contains(person.id)
                    val isCurrentlyAssigned = updatedDays.contains(dayOfWeek)
                    
                    if (shouldBeAssigned && !isCurrentlyAssigned) {
                        updatedDays.add(dayOfWeek)
                        repository.updatePerson(person.copy(dayOfWeekAssignment = updatedDays))
                    } else if (!shouldBeAssigned && isCurrentlyAssigned) {
                        updatedDays.remove(dayOfWeek)
                        repository.updatePerson(person.copy(dayOfWeekAssignment = updatedDays))
                    }
                }
                
                _uiEvent.emit(HomeEvent.PersonToggled)
            } catch (e: Exception) {
                _uiEvent.emit(HomeEvent.Error(e.message ?: "대상자 설정 실패"))
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
    object PersonToggled : HomeEvent()
    data class Error(val message: String) : HomeEvent()
}
