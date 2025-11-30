package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val repository: PrayerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val personId: Long = savedStateHandle.get<Long>("personId") ?: 0L

    private val _uiState = MutableStateFlow<PersonDetailUiState>(PersonDetailUiState.Loading)
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _prayerTopics = MutableStateFlow<List<PrayerTopic>>(emptyList())
    val prayerTopics: StateFlow<List<PrayerTopic>> = _prayerTopics.asStateFlow()

    init {
        loadPersonDetail()
        loadPrayerTopics()
    }

    private fun loadPersonDetail() {
        viewModelScope.launch {
            try {
                repository.getPersonById(personId).collect { person ->
                    if (person != null) {
                        _uiState.value = PersonDetailUiState.Success(person)
                    } else {
                        _uiState.value = PersonDetailUiState.Error("대상자를 찾을 수 없습니다")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PersonDetailUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadPrayerTopics() {
        viewModelScope.launch {
            val status = if (selectedTab.value == 0) PrayerStatus.ACTIVE else PrayerStatus.ANSWERED
            repository.getPrayerTopicsByPersonAndStatus(personId, status).collect { topics ->
                _prayerTopics.value = topics
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        loadPrayerTopics()
    }

    fun addPrayerTopic(title: String) {
        viewModelScope.launch {
            try {
                val maxPriority = _prayerTopics.value.maxOfOrNull { it.priority } ?: 0
                val topic = PrayerTopic(
                    personId = personId,
                    title = title,
                    priority = maxPriority + 1
                )
                repository.insertPrayerTopic(topic)
                _uiEvent.emit(PersonDetailEvent.TopicAdded)
            } catch (e: Exception) {
                _uiEvent.emit(PersonDetailEvent.Error(e.message ?: "추가 실패"))
            }
        }
    }

    fun updatePrayerTopicPriorities(topics: List<PrayerTopic>) {
        viewModelScope.launch {
            try {
                val updatedTopics = topics.mapIndexed { index, topic ->
                    topic.copy(priority = topics.size - index)
                }
                repository.updatePrayerTopics(updatedTopics)
            } catch (e: Exception) {
                _uiEvent.emit(PersonDetailEvent.Error(e.message ?: "우선순위 업데이트 실패"))
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
                _uiEvent.emit(PersonDetailEvent.TopicAnswered)
            } catch (e: Exception) {
                _uiEvent.emit(PersonDetailEvent.Error(e.message ?: "완료 처리 실패"))
            }
        }
    }

    fun updatePrayerTopicTitle(topic: PrayerTopic, newTitle: String) {
        viewModelScope.launch {
            try {
                val updatedTopic = topic.copy(title = newTitle)
                repository.updatePrayerTopic(updatedTopic)
                _uiEvent.emit(PersonDetailEvent.TopicUpdated)
            } catch (e: Exception) {
                _uiEvent.emit(PersonDetailEvent.Error(e.message ?: "수정 실패"))
            }
        }
    }

    fun deletePrayerTopic(topic: PrayerTopic) {
        viewModelScope.launch {
            try {
                repository.deletePrayerTopic(topic)
                _uiEvent.emit(PersonDetailEvent.TopicDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(PersonDetailEvent.Error(e.message ?: "삭제 실패"))
            }
        }
    }

    private val _uiEvent = MutableSharedFlow<PersonDetailEvent>()
    val uiEvent: SharedFlow<PersonDetailEvent> = _uiEvent.asSharedFlow()
}

sealed class PersonDetailUiState {
    object Loading : PersonDetailUiState()
    data class Success(val person: com.prayernote.app.data.local.entity.Person) : PersonDetailUiState()
    data class Error(val message: String) : PersonDetailUiState()
}

sealed class PersonDetailEvent {
    object TopicAdded : PersonDetailEvent()
    object TopicUpdated : PersonDetailEvent()
    object TopicAnswered : PersonDetailEvent()
    object TopicDeleted : PersonDetailEvent()
    data class Error(val message: String) : PersonDetailEvent()
}
