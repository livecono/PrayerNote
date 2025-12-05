package com.prayernote.app.presentation.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prayernote.app.R
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.presentation.component.CustomTimePickerDialog
import com.prayernote.app.presentation.viewmodel.SettingsEvent
import com.prayernote.app.presentation.viewmodel.SettingsViewModel
import com.prayernote.app.presentation.viewmodel.ThemeMode
import com.prayernote.app.util.RequestNotificationPermission
import com.prayernote.app.util.hasNotificationPermission
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val alarmTimes by viewModel.alarmTimes.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionRequest by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    var showAddTimePickerDialog by remember { mutableStateOf(false) }
    var showEditTimePickerDialog by remember { mutableStateOf<AlarmTime?>(null) }
    var showFirebaseBackupDialog by remember { mutableStateOf(false) }
    val canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                viewModel.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    // Firebase Backup State
    val selectedPersons by viewModel.selectedPersons.collectAsState()
    val selectedTopics by viewModel.selectedTopics.collectAsState()
    val allPersons by viewModel.allPersons.collectAsState()
    val allTopics by viewModel.allTopics.collectAsState()
    val backupInProgress by viewModel.backupInProgress.collectAsState()
    val restoreInProgress by viewModel.restoreInProgress.collectAsState()
    val backupSessions by viewModel.backupSessions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SettingsEvent.AlarmAdded -> {
                    snackbarHostState.showSnackbar("알림이 추가되었습니다")
                }
                is SettingsEvent.AlarmDeleted -> {
                    snackbarHostState.showSnackbar("알림이 삭제되었습니다")
                }
                is SettingsEvent.AlarmToggled -> {
                    // No message needed
                }
                is SettingsEvent.ThemeChanged -> {
                    snackbarHostState.showSnackbar("테마가 변경되었습니다")
                }
                is SettingsEvent.BackupRequested -> {
                    snackbarHostState.showSnackbar("백업 기능은 구현 예정입니다")
                }
                is SettingsEvent.RestoreRequested -> {
                    snackbarHostState.showSnackbar("복원 기능은 구현 예정입니다")
                }
                is SettingsEvent.FirebaseBackupSuccess -> {
                    snackbarHostState.showSnackbar("백업이 완료되었습니다 (ID: ${event.backupId})")
                    showFirebaseBackupDialog = false
                }
                is SettingsEvent.FirebaseRestoreSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is SettingsEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Notification Permission Warning
            if (!hasPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "알림 권한 필요",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "기도 알림을 받으려면 권한이 필요합니다",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(onClick = { showPermissionRequest = true }) {
                                Text("허용")
                            }
                        }
                    }
                }
            }

            // Exact Alarm Permission Warning (Android 12+)
            if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "정확한 알람 권한 필요",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "정확한 시간에 알림을 받으려면 설정에서 권한을 허용해주세요",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }) {
                                Text("설정")
                            }
                        }
                    }
                }
            }

            // Alarm Section
            item {
                Text(
                    text = stringResource(R.string.alarm_time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (alarmTimes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_alarm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(alarmTimes, key = { it.id }) { alarm ->
                    AlarmTimeItem(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onEdit = { showEditTimePickerDialog = alarm },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }

            item {
                Button(
                    onClick = { showAddTimePickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_alarm))
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Theme Section
            item {
                Text(
                    text = stringResource(R.string.theme_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Column {
                    ThemeOption(
                        text = stringResource(R.string.theme_system),
                        selected = selectedTheme == ThemeMode.SYSTEM,
                        onClick = { viewModel.selectTheme(ThemeMode.SYSTEM) }
                    )
                    ThemeOption(
                        text = stringResource(R.string.theme_light),
                        selected = selectedTheme == ThemeMode.LIGHT,
                        onClick = { viewModel.selectTheme(ThemeMode.LIGHT) }
                    )
                    ThemeOption(
                        text = stringResource(R.string.theme_dark),
                        selected = selectedTheme == ThemeMode.DARK,
                        onClick = { viewModel.selectTheme(ThemeMode.DARK) }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Backup Section
            item {
                Text(
                    text = stringResource(R.string.backup_restore),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Button(
                    onClick = { showFirebaseBackupDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("백업")
                }
            }

            item {
                BackupSessionListSection(
                    sessions = backupSessions,
                    restoreInProgress = restoreInProgress,
                    onRestoreClick = { backupId ->
                        viewModel.restoreFromBackup(backupId)
                    }
                )
            }
        }
    }

    if (showPermissionRequest) {
        RequestNotificationPermission { granted ->
            hasPermission = granted
            showPermissionRequest = false
        }
    }

    if (showAddTimePickerDialog) {
        CustomTimePickerDialog(
            onDismiss = { showAddTimePickerDialog = false },
            onConfirm = { hour, minute ->
                viewModel.addAlarm(hour, minute)
            }
        )
    }

    showEditTimePickerDialog?.let { alarm ->
        CustomTimePickerDialog(
            initialHour = alarm.hour,
            initialMinute = alarm.minute,
            onDismiss = { showEditTimePickerDialog = null },
            onConfirm = { hour, minute ->
                viewModel.updateAlarmTime(alarm, hour, minute)
            }
        )
    }

    if (showFirebaseBackupDialog) {
        FirebaseBackupDialog(
            allPersons = allPersons,
            allTopics = allTopics,
            selectedPersons = selectedPersons,
            selectedTopics = selectedTopics,
            backupInProgress = backupInProgress,
            onPersonToggle = { viewModel.togglePersonSelection(it) },
            onTopicToggle = { viewModel.toggleTopicSelection(it) },
            onSelectAllPersons = { viewModel.selectAllPersons() },
            onClearPersons = { viewModel.clearPersonSelection() },
            onSelectAllTopics = { viewModel.selectAllTopics() },
            onClearTopics = { viewModel.clearTopicSelection() },
            onBackupClick = { 
                viewModel.backupToFirebase()
                // 백업 시작 시 다이얼로그 즉시 닫기
                showFirebaseBackupDialog = false
            },
            onDismiss = { 
                showFirebaseBackupDialog = false
                viewModel.clearPersonSelection()
                viewModel.clearTopicSelection()
            }
        )
    }
}

@Composable
fun AlarmTimeItem(
    alarm: AlarmTime,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onEdit) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
