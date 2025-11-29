package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.domain.repository.PrayerRepository
import com.prayernote.app.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val alarmTimes: StateFlow<List<AlarmTime>> = repository.getAllAlarms()
        .catch { exception ->
            _uiState.value = SettingsUiState.Error(exception.message ?: "알 수 없는 오류")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showAlarmDialog = MutableStateFlow(false)
    val showAlarmDialog: StateFlow<Boolean> = _showAlarmDialog.asStateFlow()

    private val _selectedTheme = MutableStateFlow(ThemeMode.SYSTEM)
    val selectedTheme: StateFlow<ThemeMode> = _selectedTheme.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun showAlarmDialog() {
        _showAlarmDialog.value = true
    }

    fun hideAlarmDialog() {
        _showAlarmDialog.value = false
    }

    fun addAlarm(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val alarm = AlarmTime(hour = hour, minute = minute, enabled = true)
                val alarmId = repository.insertAlarm(alarm)
                val insertedAlarm = alarm.copy(id = alarmId)
                alarmScheduler.scheduleAlarm(insertedAlarm)
                _uiEvent.emit(SettingsEvent.AlarmAdded)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "알림 추가 실패"))
            }
        }
    }

    fun updateAlarm(alarm: AlarmTime) {
        viewModelScope.launch {
            try {
                repository.updateAlarm(alarm)
                alarmScheduler.scheduleAlarm(alarm)
                _uiEvent.emit(SettingsEvent.AlarmUpdated)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "알림 업데이트 실패"))
            }
        }
    }

    fun deleteAlarm(alarm: AlarmTime) {
        viewModelScope.launch {
            try {
                alarmScheduler.cancelAlarm(alarm)
                repository.deleteAlarm(alarm)
                _uiEvent.emit(SettingsEvent.AlarmDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "알림 삭제 실패"))
            }
        }
    }

    fun toggleAlarm(alarm: AlarmTime) {
        viewModelScope.launch {
            try {
                val updatedAlarm = alarm.copy(enabled = !alarm.enabled)
                repository.updateAlarm(updatedAlarm)
                if (updatedAlarm.enabled) {
                    alarmScheduler.scheduleAlarm(updatedAlarm)
                } else {
                    alarmScheduler.cancelAlarm(updatedAlarm)
                }
                _uiEvent.emit(SettingsEvent.AlarmToggled)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "알림 토글 실패"))
            }
        }
    }

    fun selectTheme(theme: ThemeMode) {
        _selectedTheme.value = theme
        viewModelScope.launch {
            // Save to DataStore
            _uiEvent.emit(SettingsEvent.ThemeChanged)
        }
    }

    fun backupToJson() {
        viewModelScope.launch {
            try {
                // BackupManager will handle this
                _uiEvent.emit(SettingsEvent.BackupRequested)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "백업 실패"))
            }
        }
    }

    fun backupToCsv() {
        viewModelScope.launch {
            try {
                // BackupManager will handle this
                _uiEvent.emit(SettingsEvent.CsvExportRequested)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "CSV 내보내기 실패"))
            }
        }
    }

    fun restoreFromJson() {
        viewModelScope.launch {
            try {
                // BackupManager will handle this
                _uiEvent.emit(SettingsEvent.RestoreRequested)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "복원 실패"))
            }
        }
    }

    private val _uiEvent = MutableSharedFlow<SettingsEvent>()
    val uiEvent: SharedFlow<SettingsEvent> = _uiEvent.asSharedFlow()
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    object Success : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

sealed class SettingsEvent {
    object AlarmAdded : SettingsEvent()
    object AlarmUpdated : SettingsEvent()
    object AlarmDeleted : SettingsEvent()
    object AlarmToggled : SettingsEvent()
    object ThemeChanged : SettingsEvent()
    object BackupRequested : SettingsEvent()
    object CsvExportRequested : SettingsEvent()
    object RestoreRequested : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}
