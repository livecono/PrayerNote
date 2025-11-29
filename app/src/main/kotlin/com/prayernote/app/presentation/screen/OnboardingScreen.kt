package com.prayernote.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayernote.app.R
import com.prayernote.app.data.datastore.PreferencesDataStore
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    preferencesDataStore: PreferencesDataStore,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_welcome_title),
                    description = stringResource(R.string.onboarding_welcome_description)
                )
                1 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_features_title),
                    description = stringResource(R.string.onboarding_features_description)
                )
                2 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_notification_title),
                    description = stringResource(R.string.onboarding_notification_description)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text("이전")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == pagerState.currentPage) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                        } else {
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            ) {}
                        }
                    }
                }
            }

            if (pagerState.currentPage < 2) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) {
                    Text(stringResource(R.string.next))
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            preferencesDataStore.setFirstLaunchCompleted()
                            onComplete()
                        }
                    }
                ) {
                    Text(stringResource(R.string.start))
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
