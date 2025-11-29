package com.prayernote.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prayernote.app.R
import com.prayernote.app.data.local.dao.PersonWithDay
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.presentation.viewmodel.DayAssignmentEvent
import com.prayernote.app.presentation.viewmodel.DayAssignmentViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAssignmentScreen(
    viewModel: DayAssignmentViewModel = hiltViewModel()
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val assignedPersons by viewModel.assignedPersons.collectAsState()
    val showDialog by viewModel.showPersonSelectDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val dayNames = listOf(
        stringResource(R.string.sunday),
        stringResource(R.string.monday),
        stringResource(R.string.tuesday),
        stringResource(R.string.wednesday),
        stringResource(R.string.thursday),
        stringResource(R.string.friday),
        stringResource(R.string.saturday)
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is DayAssignmentEvent.PersonAssigned -> {
                    snackbarHostState.showSnackbar("대상자가 할당되었습니다")
                }
                is DayAssignmentEvent.PersonUnassigned -> {
                    snackbarHostState.showSnackbar("할당이 해제되었습니다")
                }
                is DayAssignmentEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.day_assignment)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showPersonSelectDialog() }
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.assign_person))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Day Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedDay,
                edgePadding = 0.dp
            ) {
                dayNames.forEachIndexed { index, day ->
                    Tab(
                        selected = selectedDay == index,
                        onClick = { viewModel.selectDay(index) },
                        text = { Text(day) }
                    )
                }
            }

            // Assigned Persons List
            if (assignedPersons.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_assignment),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(assignedPersons, key = { it.person.id }) { personWithDay ->
                        AssignedPersonItem(
                            person = personWithDay.person,
                            onUnassign = { viewModel.unassignPerson(personWithDay.person.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        PersonSelectDialog(
            availablePersons = viewModel.getAvailablePersons(),
            onDismiss = { viewModel.hidePersonSelectDialog() },
            onSelect = { personId ->
                viewModel.assignPerson(personId)
                viewModel.hidePersonSelectDialog()
            }
        )
    }
}

@Composable
fun AssignedPersonItem(
    person: Person,
    onUnassign: () -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (person.memo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = person.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onUnassign) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "할당 해제",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PersonSelectDialog(
    availablePersons: List<Person>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.assign_person)) },
        text = {
            if (availablePersons.isEmpty()) {
                Text("할당 가능한 대상자가 없습니다")
            } else {
                LazyColumn {
                    items(availablePersons, key = { it.id }) { person ->
                        TextButton(
                            onClick = { onSelect(person.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = person.name,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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
