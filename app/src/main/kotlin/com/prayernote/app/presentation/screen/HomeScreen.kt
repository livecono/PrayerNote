package com.prayernote.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prayernote.app.R
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.presentation.viewmodel.HomeEvent
import com.prayernote.app.presentation.viewmodel.HomeUiState
import com.prayernote.app.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPersonClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val todayPrayers by viewModel.todayPrayers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPersonSelectionDialog by remember { mutableStateOf(false) }
    
    // Random prayer quote
    val prayerQuotes = context.resources.getStringArray(R.array.prayer_quotes)
    val randomQuote = remember { 
        prayerQuotes.random().split("|").let { 
            it[0] to it.getOrNull(1).orEmpty() 
        }
    }

    val dayNames = listOf("주일", "월", "화", "수", "목", "금", "토")
    val calendar = Calendar.getInstance()
    val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeEvent.TopicAnswered -> {
                    snackbarHostState.showSnackbar("응답 완료되었습니다")
                }
                is HomeEvent.PersonToggled -> {
                    snackbarHostState.showSnackbar("오늘 기도 대상자가 변경되었습니다")
                }
                is HomeEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.nav_home))
                        Text(
                            text = if (todayDayOfWeek == 0) "오늘은 ${dayNames[todayDayOfWeek]}" else "오늘은 ${dayNames[todayDayOfWeek]}요일",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPersonSelectionDialog = true }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "오늘 기도할 대상자 선택")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Prayer Quote Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "\"${randomQuote.first}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (randomQuote.second.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "- ${randomQuote.second}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Content
            Box(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    is HomeUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is HomeUiState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "오늘 기도할 대상자가 없습니다",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "요일별 탭에서 대상자를 할당해주세요",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is HomeUiState.Success, is HomeUiState.Error -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                    todayPrayers.forEach { (person, topics) ->
                        item(key = "header_${person.id}") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { onPersonClick(person.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = person.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (person.memo.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = person.memo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${topics.size}개",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        items(topics, key = { it.id }) { topic ->
                            PrayerTopicCard(
                                topic = topic,
                                onMarkAsAnswered = { viewModel.markAsAnswered(topic) }
                            )
                        }

                        item(key = "spacer_${person.id}") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
                }
            }
        }
    }

    if (showPersonSelectionDialog) {
        TodayPersonSelectionDialog(
            onDismiss = { showPersonSelectionDialog = false },
            todayPersonIds = todayPrayers.keys.map { it.id }.toSet(),
            onConfirm = { selectedPersonIds ->
                viewModel.updateTodayPersons(selectedPersonIds)
                showPersonSelectionDialog = false
            }
        )
    }
}

@Composable
fun TodayPersonSelectionDialog(
    onDismiss: () -> Unit,
    todayPersonIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    personListViewModel: com.prayernote.app.presentation.viewmodel.PersonListViewModel = hiltViewModel()
) {
    val persons by personListViewModel.persons.collectAsState()
    var selectedPersonIds by remember { mutableStateOf(todayPersonIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("오늘 기도할 대상자 선택") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(persons, key = { it.id }) { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedPersonIds = if (selectedPersonIds.contains(person.id)) {
                                    selectedPersonIds - person.id
                                } else {
                                    selectedPersonIds + person.id
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedPersonIds.contains(person.id),
                            onCheckedChange = { checked ->
                                selectedPersonIds = if (checked) {
                                    selectedPersonIds + person.id
                                } else {
                                    selectedPersonIds - person.id
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (person.memo.isNotEmpty()) {
                                Text(
                                    text = person.memo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPersonIds) }) {
                Text("완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun PrayerTopicCard(
    topic: PrayerTopic,
    onMarkAsAnswered: () -> Unit
) {
    var showOptionsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { showOptionsDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("기도 응답") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "이 기도가 응답되었나요?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOptionsDialog = false
                        onMarkAsAnswered()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("응답 완료")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
