package com.prayernote.app.presentation.screen

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.prayernote.app.R
import com.prayernote.app.presentation.viewmodel.StatisticsPeriod
import com.prayernote.app.presentation.viewmodel.StatisticsUiState
import com.prayernote.app.presentation.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val totalPrayerCount by viewModel.totalPrayerCount.collectAsState()
    val answerRate by viewModel.answerRate.collectAsState()
    val monthlyPrayerCount by viewModel.monthlyPrayerCount.collectAsState()
    val prayerStatisticsByPerson by viewModel.prayerStatisticsByPerson.collectAsState()

    var showPeriodMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is StatisticsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_statistics_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is StatisticsUiState.Success, is StatisticsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Period Selector
                    Box {
                        OutlinedButton(
                            onClick = { showPeriodMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = getPeriodText(selectedPeriod))
                        }
                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_1_month)) },
                                onClick = {
                                    viewModel.selectPeriod(StatisticsPeriod.ONE_MONTH)
                                    showPeriodMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_3_months)) },
                                onClick = {
                                    viewModel.selectPeriod(StatisticsPeriod.THREE_MONTHS)
                                    showPeriodMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_6_months)) },
                                onClick = {
                                    viewModel.selectPeriod(StatisticsPeriod.SIX_MONTHS)
                                    showPeriodMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.period_1_year)) },
                                onClick = {
                                    viewModel.selectPeriod(StatisticsPeriod.ONE_YEAR)
                                    showPeriodMenu = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatisticCard(
                            title = stringResource(R.string.total_prayers),
                            value = totalPrayerCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            title = stringResource(R.string.answer_rate),
                            value = "${answerRate.toInt()}%",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Monthly Prayer Count Bar Chart
                    if (monthlyPrayerCount.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.monthly_prayer_count),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    description.isEnabled = false
                                    setDrawGridBackground(false)
                                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                                    axisRight.isEnabled = false
                                    legend.isEnabled = false
                                }
                            },
                            update = { chart ->
                                val entries = monthlyPrayerCount.mapIndexed { index, data ->
                                    BarEntry(index.toFloat(), data.count.toFloat())
                                }
                                val dataSet = BarDataSet(entries, "기도 횟수").apply {
                                    color = Color.parseColor("#5E6DC7")
                                    valueTextSize = 10f
                                }
                                chart.data = BarData(dataSet)
                                chart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Prayer by Person Pie Chart
                    if (prayerStatisticsByPerson.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.prayer_by_person),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        AndroidView(
                            factory = { context ->
                                PieChart(context).apply {
                                    description.isEnabled = false
                                    setDrawEntryLabels(true)
                                    legend.isEnabled = true
                                }
                            },
                            update = { chart ->
                                val entries = prayerStatisticsByPerson.map { stat ->
                                    PieEntry(stat.prayerCount.toFloat(), stat.personName)
                                }
                                val dataSet = PieDataSet(entries, "").apply {
                                    colors = listOf(
                                        Color.parseColor("#5E6DC7"),
                                        Color.parseColor("#8B6E4E"),
                                        Color.parseColor("#7D5260"),
                                        Color.parseColor("#6B9080"),
                                        Color.parseColor("#A8DADC")
                                    )
                                    valueTextSize = 12f
                                    valueTextColor = Color.WHITE
                                }
                                chart.data = PieData(dataSet)
                                chart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun getPeriodText(period: StatisticsPeriod): String {
    return when (period) {
        StatisticsPeriod.ONE_MONTH -> stringResource(R.string.period_1_month)
        StatisticsPeriod.THREE_MONTHS -> stringResource(R.string.period_3_months)
        StatisticsPeriod.SIX_MONTHS -> stringResource(R.string.period_6_months)
        StatisticsPeriod.ONE_YEAR -> stringResource(R.string.period_1_year)
    }
}
