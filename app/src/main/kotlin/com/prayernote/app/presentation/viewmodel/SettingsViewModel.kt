package com.prayernote.app.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayernote.app.data.datastore.PreferencesDataStore
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.domain.repository.BackupRepository
import com.prayernote.app.domain.repository.PrayerRepository
import com.prayernote.app.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val alarmScheduler: AlarmScheduler,
    private val backupRepository: BackupRepository?,
    private val preferencesDataStore: PreferencesDataStore
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

    // Firebase 백업 관련 상태
    private val _backupInProgress = MutableStateFlow(false)
    val backupInProgress: StateFlow<Boolean> = _backupInProgress.asStateFlow()

    private val _restoreInProgress = MutableStateFlow(false)
    val restoreInProgress: StateFlow<Boolean> = _restoreInProgress.asStateFlow()

    private val _backupSessions = MutableStateFlow<List<com.prayernote.app.data.firebase.model.BackupSession>>(emptyList())
    val backupSessions: StateFlow<List<com.prayernote.app.data.firebase.model.BackupSession>> = _backupSessions.asStateFlow()

    private val _selectedPersons = MutableStateFlow<List<Person>>(emptyList())
    val selectedPersons: StateFlow<List<Person>> = _selectedPersons.asStateFlow()

    private val _selectedTopics = MutableStateFlow<List<PrayerTopic>>(emptyList())
    val selectedTopics: StateFlow<List<PrayerTopic>> = _selectedTopics.asStateFlow()

    val allPersons: StateFlow<List<Person>> = repository.getAllPersons()
        .catch { exception ->
            Log.e("SettingsViewModel", "Failed to load persons", exception)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 선택된 대상자와 함께 기도제목을 필터링
    val filteredTopics: StateFlow<List<PrayerTopic>> = selectedPersons
        .flatMapLatest { persons ->
            if (persons.isEmpty()) {
                flowOf(emptyList())
            } else {
                // 선택된 모든 대상자의 기도제목을 로드
                combine(
                    *persons.map { person ->
                        repository.getPrayerTopicsByPerson(person.id)
                    }.toTypedArray()
                ) { arrays ->
                    arrays.filterIsInstance<List<PrayerTopic>>().flatten()
                }
            }
        }
        .catch { exception ->
            Log.e("SettingsViewModel", "Failed to load topics", exception)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTopics: StateFlow<List<PrayerTopic>> = filteredTopics

    init {
        loadSettings()
        loadBackupSessions()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load theme from DataStore
                preferencesDataStore.themeMode.collect { theme ->
                    _selectedTheme.value = theme
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading theme", e)
            }
        }
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadBackupSessions() {
        viewModelScope.launch {
            try {
                val result = backupRepository?.getBackupSessions()
                result?.onSuccess { sessions ->
                    _backupSessions.value = sessions
                    Log.d("SettingsViewModel", "Loaded ${sessions.size} backup sessions")
                }?.onFailure { exception ->
                    Log.e("SettingsViewModel", "Failed to load backup sessions", exception)
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Exception loading backup sessions", e)
            }
        }
    }

    fun reloadBackupSessions() {
        loadBackupSessions()
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

    fun updateAlarmTime(alarm: AlarmTime, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val updatedAlarm = alarm.copy(hour = hour, minute = minute)
                repository.updateAlarm(updatedAlarm)
                alarmScheduler.scheduleAlarm(updatedAlarm)
                _uiEvent.emit(SettingsEvent.AlarmUpdated)
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "알림 시간 업데이트 실패"))
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
            preferencesDataStore.setThemeMode(theme)
            _uiEvent.emit(SettingsEvent.ThemeChanged)
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return alarmScheduler.canScheduleExactAlarms()
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

    // Firebase 백업 관련 메서드
    fun togglePersonSelection(person: Person) {
        val current = _selectedPersons.value.toMutableList()
        if (current.contains(person)) {
            current.remove(person)
        } else {
            current.add(person)
        }
        _selectedPersons.value = current
    }

    fun toggleTopicSelection(topic: PrayerTopic) {
        val current = _selectedTopics.value.toMutableList()
        if (current.contains(topic)) {
            current.remove(topic)
        } else {
            current.add(topic)
        }
        _selectedTopics.value = current
    }

    fun clearPersonSelection() {
        _selectedPersons.value = emptyList()
    }

    fun clearTopicSelection() {
        _selectedTopics.value = emptyList()
    }

    fun selectAllPersons() {
        _selectedPersons.value = allPersons.value
    }

    fun selectAllTopics() {
        _selectedTopics.value = allTopics.value
    }

    fun backupToFirebase() {
        if (_selectedPersons.value.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(SettingsEvent.Error("백업할 대상자를 선택해주세요"))
            }
            return
        }

        viewModelScope.launch {
            try {
                _backupInProgress.value = true
                
                val result = backupRepository?.backupSelectedData(
                    persons = _selectedPersons.value,
                    topics = _selectedTopics.value,
                    includeAnswered = true
                )

                result?.onSuccess { backupId ->
                    Log.d("SettingsViewModel", "Backup successful: $backupId")
                    _selectedPersons.value = emptyList()
                    _selectedTopics.value = emptyList()
                    loadBackupSessions() // 백업 후 세션 목록 새로고침
                    viewModelScope.launch {
                        _uiEvent.emit(SettingsEvent.FirebaseBackupSuccess(backupId))
                    }
                }?.onFailure { exception ->
                    Log.e("SettingsViewModel", "Backup failed", exception)
                    viewModelScope.launch {
                        _uiEvent.emit(SettingsEvent.Error(exception.message ?: "Firebase 백업 실패"))
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Exception during backup", e)
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "Firebase 백업 중 오류 발생"))
            } finally {
                _backupInProgress.value = false
            }
        }
    }

    fun restoreFromBackup(backupId: String) {
        viewModelScope.launch {
            try {
                _restoreInProgress.value = true
                
                val result = backupRepository?.restoreBackupData(backupId)

                result?.onSuccess { message ->
                    Log.d("SettingsViewModel", "Restore successful: $message")
                    loadBackupSessions() // 복원 후 세션 목록 새로고침
                    viewModelScope.launch {
                        _uiEvent.emit(SettingsEvent.FirebaseRestoreSuccess(message))
                    }
                }?.onFailure { exception ->
                    Log.e("SettingsViewModel", "Restore failed", exception)
                    viewModelScope.launch {
                        _uiEvent.emit(SettingsEvent.Error(exception.message ?: "Firebase 복원 실패"))
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Exception during restore", e)
                _uiEvent.emit(SettingsEvent.Error(e.message ?: "Firebase 복원 중 오류 발생"))
            } finally {
                _restoreInProgress.value = false
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
    data class FirebaseBackupSuccess(val backupId: String) : SettingsEvent()
    data class FirebaseRestoreSuccess(val message: String) : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}
