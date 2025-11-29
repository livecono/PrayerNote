package com.prayernote.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prayernote.app.R
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.presentation.viewmodel.AnsweredPrayersUiState
import com.prayernote.app.presentation.viewmodel.AnsweredPrayersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnsweredPrayersScreen(
    viewModel: AnsweredPrayersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val answeredPrayers by viewModel.answeredPrayers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_answered)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is AnsweredPrayersUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AnsweredPrayersUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "응답된 기도가 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is AnsweredPrayersUiState.Success, is AnsweredPrayersUiState.Error -> {
                val groupedPrayers = viewModel.getGroupedByMonth()
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    groupedPrayers.forEach { (month, prayers) ->
                        item(key = month) {
                            Text(
                                text = month,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(prayers, key = { it.id }) { prayer ->
                            AnsweredPrayerItem(prayer = prayer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnsweredPrayerItem(prayer: PrayerTopic) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = prayer.title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (prayer.answeredAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "응답: ${com.prayernote.app.presentation.screen.formatDate(prayer.answeredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
