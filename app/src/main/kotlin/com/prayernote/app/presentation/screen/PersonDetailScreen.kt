package com.prayernote.app.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
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
    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is PersonDetailEvent.TopicAdded -> {
                    snackbarHostState.showSnackbar(
                        message = "기도제목이 추가되었습니다",
                        duration = SnackbarDuration.Short
                    )
                    showAddDialog = false
                }
                is PersonDetailEvent.TopicUpdated -> {
                    snackbarHostState.showSnackbar(
                        message = "기도제목이 수정되었습니다",
                        duration = SnackbarDuration.Short
                    )
                }
                is PersonDetailEvent.TopicAnswered -> {
                    snackbarHostState.showSnackbar(
                        message = "응답 완료되었습니다",
                        duration = SnackbarDuration.Short
                    )
                }
                is PersonDetailEvent.TopicDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "기도제목이 삭제되었습니다",
                        duration = SnackbarDuration.Short
                    )
                }
                is PersonDetailEvent.TopicRestored -> {
                    snackbarHostState.showSnackbar(
                        message = "진행 중으로 복원되었습니다",
                        duration = SnackbarDuration.Short
                    )
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
            if (selectedTab == 0 && !isScrolling) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_prayer_topic))
                }
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
                                onEdit = { topic, newTitle -> viewModel.updatePrayerTopicTitle(topic, newTitle) },
                                onDelete = { viewModel.deletePrayerTopic(it) },
                                onScrollingChanged = { isScrolling = it }
                            )
                        } else {
                            AnsweredPrayerList(
                                topics = prayerTopics,
                                onEdit = { topic, newTitle -> viewModel.updatePrayerTopicTitle(topic, newTitle) },
                                onRestore = { viewModel.restoreTopic(it) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderablePrayerList(
    topics: List<PrayerTopic>,
    onReorder: (List<PrayerTopic>) -> Unit,
    onMarkAsAnswered: (PrayerTopic) -> Unit,
    onEdit: (PrayerTopic, String) -> Unit,
    onDelete: (PrayerTopic) -> Unit,
    onScrollingChanged: (Boolean) -> Unit
) {
    var items by remember(topics) { mutableStateOf(topics) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        items = items.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        onScrollingChanged(lazyListState.isScrollInProgress)
    }

    LaunchedEffect(items) {
        if (items != topics) {
            onReorder(items)
        }
    }

    LazyColumn(
        state = lazyListState,
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
                    onEdit = { newTitle -> onEdit(topic, newTitle) },
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
    onEdit: (PrayerTopic, String) -> Unit,
    onRestore: (PrayerTopic) -> Unit,
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
                onEdit = { newTitle -> onEdit(topic, newTitle) },
                onRestore = { onRestore(topic) },
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
    onEdit: (String) -> Unit,
    onRestore: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

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
                .clickable { showEditDialog = true }
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "등록일: ${formatDate(topic.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (topic.status == PrayerStatus.ANSWERED && topic.answeredAt != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "응답: ${formatDate(topic.answeredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        PrayerTopicEditDialog(
            topic = topic,
            showAnswerButton = showAnswerButton,
            onDismiss = { showEditDialog = false },
            onConfirm = { newTitle ->
                showEditDialog = false
                onEdit(newTitle)
            },
            onMarkAsAnswered = {
                showEditDialog = false
                onMarkAsAnswered()
            },
            onRestore = {
                showEditDialog = false
                onRestore()
            },
            onDelete = {
                showEditDialog = false
                onDelete()
            }
        )
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
                        onDismiss()
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

@Composable
fun PrayerTopicEditDialog(
    topic: PrayerTopic,
    showAnswerButton: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onMarkAsAnswered: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(topic.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기도제목 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.prayer_topic)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (showAnswerButton) {
                        OutlinedButton(
                            onClick = onMarkAsAnswered,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("응답")
                        }
                    } else {
                        // 응답됨 탭에서는 복원 버튼 표시
                        OutlinedButton(
                            onClick = onRestore,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("복원")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("삭제")
                    }
                }
            }
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

@Composable
fun EditPrayerTopicDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기도제목 수정") },
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
                        onDismiss()
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

@Composable
fun PrayerTopicOptionsDialog(
    showAnswerButton: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkAsAnswered: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기도제목 옵션") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("수정", modifier = Modifier.weight(1f))
                }
                
                if (showAnswerButton) {
                    TextButton(
                        onClick = onMarkAsAnswered,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("응답 완료", modifier = Modifier.weight(1f))
                    }
                }
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("삭제", modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {},
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
