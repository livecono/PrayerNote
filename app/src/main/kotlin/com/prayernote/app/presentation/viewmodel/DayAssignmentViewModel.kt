package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayernote.app.data.local.dao.PersonWithDay
import com.prayernote.app.data.local.entity.DayAssignment
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DayAssignmentViewModel @Inject constructor(
    private val repository: PrayerRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(0)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _assignedPersons = MutableStateFlow<List<PersonWithDay>>(emptyList())
    val assignedPersons: StateFlow<List<PersonWithDay>> = _assignedPersons.asStateFlow()

    private val _allPersons = MutableStateFlow<List<Person>>(emptyList())
    val allPersons: StateFlow<List<Person>> = _allPersons.asStateFlow()

    private val _showPersonSelectDialog = MutableStateFlow(false)
    val showPersonSelectDialog: StateFlow<Boolean> = _showPersonSelectDialog.asStateFlow()

    init {
        loadAllPersons()
        loadAssignedPersons()
    }

    private fun loadAllPersons() {
        viewModelScope.launch {
            repository.getAllPersons().collect { persons ->
                _allPersons.value = persons
            }
        }
    }

    private fun loadAssignedPersons() {
        viewModelScope.launch {
            repository.getPersonsByDay(selectedDay.value).collect { persons ->
                _assignedPersons.value = persons
            }
        }
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day
        loadAssignedPersons()
    }

    fun showPersonSelectDialog() {
        _showPersonSelectDialog.value = true
    }

    fun hidePersonSelectDialog() {
        _showPersonSelectDialog.value = false
    }

    fun assignPerson(personId: Long) {
        viewModelScope.launch {
            try {
                val assignment = DayAssignment(
                    personId = personId,
                    dayOfWeek = selectedDay.value
                )
                repository.insertAssignment(assignment)
                _uiEvent.emit(DayAssignmentEvent.PersonAssigned)
            } catch (e: Exception) {
                _uiEvent.emit(DayAssignmentEvent.Error(e.message ?: "할당 실패"))
            }
        }
    }

    fun unassignPerson(personId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAssignment(personId, selectedDay.value)
                _uiEvent.emit(DayAssignmentEvent.PersonUnassigned)
            } catch (e: Exception) {
                _uiEvent.emit(DayAssignmentEvent.Error(e.message ?: "할당 해제 실패"))
            }
        }
    }

    fun getAvailablePersons(): List<Person> {
        val assignedIds = assignedPersons.value.map { it.person.id }.toSet()
        return allPersons.value.filter { it.id !in assignedIds }
    }

    private val _uiEvent = MutableSharedFlow<DayAssignmentEvent>()
    val uiEvent: SharedFlow<DayAssignmentEvent> = _uiEvent.asSharedFlow()
}

sealed class DayAssignmentEvent {
    object PersonAssigned : DayAssignmentEvent()
    object PersonUnassigned : DayAssignmentEvent()
    data class Error(val message: String) : DayAssignmentEvent()
}
