package com.prayernote.app.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerTopic

@Composable
fun FirebaseBackupDialog(
    allPersons: List<Person>,
    allTopics: List<PrayerTopic>,
    selectedPersons: List<Person>,
    selectedTopics: List<PrayerTopic>,
    backupInProgress: Boolean,
    onPersonToggle: (Person) -> Unit,
    onTopicToggle: (PrayerTopic) -> Unit,
    onSelectAllPersons: () -> Unit,
    onClearPersons: () -> Unit,
    onSelectAllTopics: () -> Unit,
    onClearTopics: () -> Unit,
    onBackupClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
        title = {
            Text(
                text = "Firebase 백업",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                // 대상자 선택 섹션
                SelectionSection(
                    title = "대상자 선택",
                    itemCount = allPersons.size,
                    selectedCount = selectedPersons.size,
                    onSelectAll = onSelectAllPersons,
                    onClear = onClearPersons,
                    expandedContent = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(allPersons) { person ->
                                PersonSelectionItem(
                                    person = person,
                                    isSelected = selectedPersons.contains(person),
                                    onToggle = { onPersonToggle(person) }
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 기도제목 선택 섹션
                SelectionSection(
                    title = "기도제목 선택",
                    itemCount = allTopics.size,
                    selectedCount = selectedTopics.size,
                    onSelectAll = onSelectAllTopics,
                    onClear = onClearTopics,
                    expandedContent = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(allTopics) { topic ->
                                PrayerTopicSelectionItem(
                                    topic = topic,
                                    isSelected = selectedTopics.contains(topic),
                                    allPersons = allPersons,
                                    onToggle = { onTopicToggle(topic) }
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 정보 메시지
                Text(
                    text = "${selectedPersons.size}명의 대상자와 ${selectedTopics.size}개의 기도제목이 백업됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBackupClick,
                enabled = selectedPersons.isNotEmpty() && !backupInProgress,
                modifier = Modifier
                    .height(40.dp)
            ) {
                if (backupInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("백업 중...")
                } else {
                    Text("백업")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !backupInProgress
            ) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun SelectionSection(
    title: String,
    itemCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "선택됨: $selectedCount / 전체: $itemCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // 확장 가능한 콘텐츠
        if (expanded) {
            Column(modifier = Modifier.animateContentSize()) {
                // 전체 선택/해제 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSelectAll,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("모두 선택", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("선택 해제", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 항목 목록
                expandedContent()
            }
        }
    }
}

@Composable
private fun PersonSelectionItem(
    person: Person,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (person.memo.isNotEmpty()) {
                Text(
                    text = person.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PrayerTopicSelectionItem(
    topic: PrayerTopic,
    isSelected: Boolean,
    allPersons: List<Person>,
    onToggle: () -> Unit
) {
    val personName = allPersons.find { it.id == topic.personId }?.name ?: "Unknown"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2
            )
            Text(
                text = "대상자: $personName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BackupSessionListSection(
    sessions: List<com.prayernote.app.data.firebase.model.BackupSession>,
    restoreInProgress: Boolean,
    onRestoreClick: (String) -> Unit
) {
    if (sessions.isEmpty()) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "백업 이력",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(sessions) { session ->
                BackupSessionCard(
                    session = session,
                    restoreInProgress = restoreInProgress,
                    onRestoreClick = { onRestoreClick(session.id) }
                )
            }
        }
    }
}

@Composable
fun BackupSessionCard(
    session: com.prayernote.app.data.firebase.model.BackupSession,
    restoreInProgress: Boolean,
    onRestoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "대상자: ${session.totalPersons}명 | 기도: ${session.totalTopics}개",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDate(session.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onRestoreClick,
                enabled = !restoreInProgress,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (restoreInProgress) "복원 중..." else "복원")
            }
        }
    }
}

private fun formatDate(date: java.util.Date?): String {
    if (date == null) return "알 수 없음"
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA)
    return sdf.format(date)
}
