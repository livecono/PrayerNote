package com.prayernote.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prayernote.app.R
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.presentation.viewmodel.PersonDetailEvent
import com.prayernote.app.presentation.viewmodel.PersonDetailUiState
import com.prayernote.app.presentation.viewmodel.PersonDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: PersonDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val prayerTopics by viewModel.prayerTopics.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is PersonDetailEvent.TopicAdded -> {
                    snackbarHostState.showSnackbar("기도제목이 추가되었습니다")
                    showAddDialog = false
                }
                is PersonDetailEvent.TopicAnswered -> {
                    snackbarHostState.showSnackbar("응답 완료되었습니다")
                }
                is PersonDetailEvent.TopicDeleted -> {
                    snackbarHostState.showSnackbar("기도제목이 삭제되었습니다")
                }
                is PersonDetailEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is PersonDetailUiState.Success -> Text(state.person.name)
                        else -> Text("")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기")
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
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_prayer_topic))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.active_prayers)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text(stringResource(R.string.answered_prayers)) }
                )
            }

            // Content
            when (uiState) {
                is PersonDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PersonDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as PersonDetailUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is PersonDetailUiState.Success -> {
                    if (prayerTopics.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.prayer_topic_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        if (selectedTab == 0) {
                            ReorderablePrayerList(
                                topics = prayerTopics,
                                onReorder = { viewModel.updatePrayerTopicPriorities(it) },
                                onMarkAsAnswered = { viewModel.markAsAnswered(it) },
                                onDelete = { viewModel.deletePrayerTopic(it) }
                            )
                        } else {
                            AnsweredPrayerList(
                                topics = prayerTopics,
                                onDelete = { viewModel.deletePrayerTopic(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPrayerTopicDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                viewModel.addPrayerTopic(title)
            }
        )
    }
}

@Composable
fun ReorderablePrayerList(
    topics: List<PrayerTopic>,
    onReorder: (List<PrayerTopic>) -> Unit,
    onMarkAsAnswered: (PrayerTopic) -> Unit,
    onDelete: (PrayerTopic) -> Unit
) {
    var items by remember(topics) { mutableStateOf(topics) }
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    ) { from, to ->
        items = items.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(items) {
        if (items != topics) {
            onReorder(items)
        }
    }

    LazyColumn(
        state = reorderableLazyListState.lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(items, key = { it.id }) { topic ->
            ReorderableItem(
                state = reorderableLazyListState,
                key = topic.id
            ) { isDragging ->
                PrayerTopicItem(
                    topic = topic,
                    isDragging = isDragging,
                    showAnswerButton = true,
                    onMarkAsAnswered = { onMarkAsAnswered(topic) },
                    onDelete = { onDelete(topic) },
                    modifier = Modifier.longPressDraggableHandle()
                )
            }
        }
    }
}

@Composable
fun AnsweredPrayerList(
    topics: List<PrayerTopic>,
    onDelete: (PrayerTopic) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(topics, key = { it.id }) { topic ->
            PrayerTopicItem(
                topic = topic,
                isDragging = false,
                showAnswerButton = false,
                onMarkAsAnswered = {},
                onDelete = { onDelete(topic) }
            )
        }
    }
}

@Composable
fun PrayerTopicItem(
    topic: PrayerTopic,
    isDragging: Boolean,
    showAnswerButton: Boolean,
    onMarkAsAnswered: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAnswerButton) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "드래그",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (topic.status == PrayerStatus.ANSWERED && topic.answeredAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "응답: ${formatDate(topic.answeredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showAnswerButton) {
                IconButton(onClick = onMarkAsAnswered) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.mark_as_answered),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

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

@Composable
fun AddPrayerTopicDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_prayer_topic)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.prayer_topic)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

fun formatDate(date: java.util.Date): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.time = date
    return "${calendar.get(java.util.Calendar.YEAR)}.${calendar.get(java.util.Calendar.MONTH) + 1}.${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
}
