package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PersonListViewModel @Inject constructor(
    private val repository: PrayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonListUiState>(PersonListUiState.Loading)
    val uiState: StateFlow<PersonListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val persons: StateFlow<List<Person>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllPersons()
            } else {
                repository.searchPersons(query)
            }
        }
        .catch { exception ->
            _uiState.value = PersonListUiState.Error(exception.message ?: "알 수 없는 오류")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val personsPaged: Flow<PagingData<Person>> = repository.getAllPersonsPaged()
        .cachedIn(viewModelScope)

    init {
        loadPersons()
    }

    private fun loadPersons() {
        viewModelScope.launch {
            try {
                _uiState.value = PersonListUiState.Loading
                persons.collect { personList ->
                    _uiState.value = if (personList.isEmpty()) {
                        PersonListUiState.Empty
                    } else {
                        PersonListUiState.Success(personList)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PersonListUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addPerson(name: String, memo: String) {
        viewModelScope.launch {
            try {
                val person = Person(name = name, memo = memo)
                repository.insertPerson(person)
                _uiEvent.emit(PersonListEvent.PersonAdded)
            } catch (e: Exception) {
                _uiEvent.emit(PersonListEvent.Error(e.message ?: "추가 실패"))
            }
        }
    }

    fun updatePerson(person: Person, name: String, memo: String, dayOfWeekAssignment: Set<Int>) {
        viewModelScope.launch {
            try {
                val updatedPerson = person.copy(
                    name = name,
                    memo = memo,
                    dayOfWeekAssignment = dayOfWeekAssignment
                )
                repository.updatePerson(updatedPerson)
                _uiEvent.emit(PersonListEvent.PersonUpdated)
            } catch (e: Exception) {
                _uiEvent.emit(PersonListEvent.Error(e.message ?: "수정 실패"))
            }
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            try {
                repository.deletePerson(person)
                _uiEvent.emit(PersonListEvent.PersonDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(PersonListEvent.Error(e.message ?: "삭제 실패"))
            }
        }
    }

    fun addPrayerTopicToPerson(personId: Long, topic: String) {
        viewModelScope.launch {
            try {
                val prayerTopic = PrayerTopic(
                    personId = personId,
                    title = topic,
                    status = PrayerStatus.ACTIVE,
                    createdAt = Date(),
                    priority = 0  // Will be set by repository
                )
                repository.insertPrayerTopic(prayerTopic)
                _uiEvent.emit(PersonListEvent.PrayerTopicAdded)
            } catch (e: Exception) {
                _uiEvent.emit(PersonListEvent.Error(e.message ?: "기도제목 추가 실패"))
            }
        }
    }

    private val _uiEvent = MutableSharedFlow<PersonListEvent>()
    val uiEvent: SharedFlow<PersonListEvent> = _uiEvent.asSharedFlow()
}

sealed class PersonListUiState {
    object Loading : PersonListUiState()
    object Empty : PersonListUiState()
    data class Success(val persons: List<Person>) : PersonListUiState()
    data class Error(val message: String) : PersonListUiState()
}

sealed class PersonListEvent {
    object PersonAdded : PersonListEvent()
    object PersonUpdated : PersonListEvent()
    object PersonDeleted : PersonListEvent()
    object PrayerTopicAdded : PersonListEvent()
    data class Error(val message: String) : PersonListEvent()
}
