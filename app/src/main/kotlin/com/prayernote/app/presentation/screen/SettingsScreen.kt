package com.prayernote.app.presentation.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
                is SettingsEvent.CsvExportRequested -> {
                    snackbarHostState.showSnackbar("CSV 내보내기 기능은 구현 예정입니다")
                }
                is SettingsEvent.RestoreRequested -> {
                    snackbarHostState.showSnackbar("복원 기능은 구현 예정입니다")
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

            // Alarm Section
            item {
                Text(
                    text = stringResource(R.string.alarm_time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }            if (alarmTimes.isEmpty()) {
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
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.addAlarm(hour, minute)
                            },
                            7, 0, true
                        ).show()
                    },
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.backupToJson() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.backup_json))
                    }
                    OutlinedButton(
                        onClick = { viewModel.backupToCsv() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.backup_csv))
                    }
                    Button(
                        onClick = { viewModel.restoreFromJson() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.restore_json))
                    }
                }
            }
        }
    }

    if (showPermissionRequest) {
        RequestNotificationPermission { granted ->
            hasPermission = granted
            showPermissionRequest = false
        }
    }
}

@Composable
fun AlarmTimeItem(
    alarm: AlarmTime,
    onToggle: () -> Unit,
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
            Text(
                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                style = MaterialTheme.typography.titleLarge
            )
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
