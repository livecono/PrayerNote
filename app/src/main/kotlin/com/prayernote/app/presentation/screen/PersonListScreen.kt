package com.prayernote.app.presentation.screen

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prayernote.app.R
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.presentation.viewmodel.PersonListEvent
import com.prayernote.app.presentation.viewmodel.PersonListUiState
import com.prayernote.app.presentation.viewmodel.PersonListViewModel
import kotlinx.coroutines.flow.collectLatest
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    onPersonClick: (Long) -> Unit,
    viewModel: PersonListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val persons by viewModel.persons.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Person?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is PersonListEvent.PersonAdded -> {
                    snackbarHostState.showSnackbar(
                        message = "대상자가 추가되었습니다",
                        duration = SnackbarDuration.Short
                    )
                    showAddDialog = false
                }
                is PersonListEvent.PersonUpdated -> {
                    snackbarHostState.showSnackbar(
                        message = "대상자가 수정되었습니다",
                        duration = SnackbarDuration.Short
                    )
                    showEditDialog = null
                }
                is PersonListEvent.PersonDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "대상자가 삭제되었습니다",
                        duration = SnackbarDuration.Short
                    )
                }
                is PersonListEvent.PrayerTopicAdded -> {
                    snackbarHostState.showSnackbar(
                        message = "기도제목이 추가되었습니다",
                        duration = SnackbarDuration.Short
                    )
                }
                is PersonListEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.person_list_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!isScrolling) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_person))
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                singleLine = true
            )

            // Content
            when (uiState) {
                is PersonListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PersonListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.person_list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is PersonListUiState.Success, is PersonListUiState.Error -> {
                    ReorderablePersonList(
                        persons = persons,
                        onReorder = { viewModel.reorderPersons(it) },
                        onPersonClick = onPersonClick,
                        onEdit = { showEditDialog = it },
                        onDelete = { viewModel.deletePerson(it) },
                        listState = listState
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPersonDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, memo ->
                viewModel.addPerson(name, memo)
            }
        )
    }

    showEditDialog?.let { person ->
        EditPersonDialog(
            person = person,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, memo, dayOfWeekAssignment ->
                viewModel.updatePerson(person, name, memo, dayOfWeekAssignment)
            },
            onDelete = {
                viewModel.deletePerson(person)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderablePersonList(
    persons: List<Person>,
    onReorder: (List<Person>) -> Unit,
    onPersonClick: (Long) -> Unit,
    onEdit: (Person) -> Unit,
    onDelete: (Person) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    var items by remember(persons) { mutableStateOf(persons) }
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        items = items.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(items) {
        if (items != persons) {
            onReorder(items)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(items, key = { it.id }) { person ->
            ReorderableItem(
                state = reorderableLazyListState,
                key = person.id
            ) { isDragging ->
                PersonListItem(
                    person = person,
                    isDragging = isDragging,
                    onClick = { onPersonClick(person.id) },
                    onEdit = { onEdit(person) },
                    onDelete = { onDelete(person) },
                    modifier = Modifier.combinedClickable(
                        onClick = { onPersonClick(person.id) },
                        onLongClick = { onEdit(person) }
                    )
                )
            }
        }
    }
}

@Composable
fun PersonListItem(
    person: Person,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (person.memo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = person.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // READ_CONTACTS permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.util.Log.d("PersonListScreen", "READ_CONTACTS permission denied")
        }
    }
    
    // Request permission on first composition
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    
    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts._ID
                    ),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                        
                        if (nameIndex >= 0) {
                            name = it.getString(nameIndex) ?: ""
                        }
                        
                        // Get contact groups
                        if (idIndex >= 0) {
                            val contactId = it.getString(idIndex)
                            val groups = getContactGroups(context, contactId)
                            if (groups.isNotEmpty()) {
                                memo = groups.joinToString(", ")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_person)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.person_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "연락처에서 선택",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(stringResource(R.string.person_memo)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), memo.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
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
fun EditPersonDialog(
    person: Person,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Set<Int>) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(person.name) }
    var memo by remember { mutableStateOf(person.memo) }
    var selectedDays by remember { mutableStateOf(person.dayOfWeekAssignment) }
    
    val dayNames = listOf("주일", "월", "화", "수", "목", "금", "토", "매일")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_person)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.person_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(stringResource(R.string.person_memo)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "기도 요일 선택",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 매일 + 주일~토요일 버튼 (한 줄)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 매일 버튼
                    Button(
                        onClick = {
                            selectedDays = if (7 in selectedDays) {
                                emptySet()
                            } else {
                                setOf(0, 1, 2, 3, 4, 5, 6, 7)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (7 in selectedDays) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (7 in selectedDays)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("매일", fontSize = 11.sp)
                    }
                    
                    // 주일~토요일 버튼
                    listOf("주일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, dayName ->
                        Button(
                            onClick = {
                                selectedDays = if (index in selectedDays) {
                                    // 개별 요일 해제
                                    (selectedDays - index) - 7  // 매일도 함께 해제
                                } else {
                                    // 개별 요일 추가
                                    val newDays = selectedDays + index
                                    // 0-6이 모두 포함되면 7(매일)도 추가
                                    if ((0..6).all { it in newDays }) {
                                        newDays + 7
                                    } else {
                                        newDays - 7  // 매일 제거
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (index in selectedDays)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (index in selectedDays)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(dayName, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), memo.trim(), selectedDays)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.delete))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

// Helper function to get contact groups
private fun getContactGroups(context: android.content.Context, contactId: String): List<String> {
    val groups = mutableSetOf<String>()  // Use Set to avoid duplicates
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        )
        
        cursor?.use {
            val groupIdIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
            while (it.moveToNext()) {
                if (groupIdIndex >= 0) {
                    val groupId = it.getString(groupIdIndex)
                    groupId?.let { id ->
                        val groupName = getGroupName(context, id)
                        // Filter out system groups like "My Contacts"
                        if (groupName.isNotEmpty() && 
                            groupName != "My Contacts" && 
                            !groupName.startsWith("System Group:")) {
                            groups.add(groupName)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return groups.toList()
}

private fun getGroupName(context: android.content.Context, groupId: String): String {
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups.TITLE),
            "${ContactsContract.Groups._ID} = ?",
            arrayOf(groupId),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val titleIndex = it.getColumnIndex(ContactsContract.Groups.TITLE)
                if (titleIndex >= 0) {
                    return it.getString(titleIndex) ?: ""
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}
