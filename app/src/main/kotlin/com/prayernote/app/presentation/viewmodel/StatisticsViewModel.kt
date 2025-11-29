package com.prayernote.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayernote.app.data.local.dao.MonthlyPrayerCount
import com.prayernote.app.data.local.dao.PrayerStatistics
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: PrayerRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.SIX_MONTHS)
    val selectedPeriod: StateFlow<StatisticsPeriod> = _selectedPeriod.asStateFlow()

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _totalPrayerCount = MutableStateFlow(0)
    val totalPrayerCount: StateFlow<Int> = _totalPrayerCount.asStateFlow()

    private val _answerRate = MutableStateFlow(0f)
    val answerRate: StateFlow<Float> = _answerRate.asStateFlow()

    private val _monthlyPrayerCount = MutableStateFlow<List<MonthlyPrayerCount>>(emptyList())
    val monthlyPrayerCount: StateFlow<List<MonthlyPrayerCount>> = _monthlyPrayerCount.asStateFlow()

    private val _prayerStatisticsByPerson = MutableStateFlow<List<PrayerStatistics>>(emptyList())
    val prayerStatisticsByPerson: StateFlow<List<PrayerStatistics>> = _prayerStatisticsByPerson.asStateFlow()

    private val _monthlyAnswerRate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val monthlyAnswerRate: StateFlow<List<Pair<String, Float>>> = _monthlyAnswerRate.asStateFlow()

    init {
        loadStatistics()
    }

    fun selectPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _uiState.value = StatisticsUiState.Loading

                val (startDate, endDate) = getDateRange(selectedPeriod.value)

                // Load total prayer count
                val totalCount = repository.getPrayerCountByDateRange(startDate, endDate)
                _totalPrayerCount.value = totalCount

                // Load answer rate
                val answerRate = repository.getAnswerRate(startDate, endDate)
                _answerRate.value = answerRate

                // Load monthly prayer count
                val monthlyCount = repository.getMonthlyPrayerCount(startDate, endDate)
                _monthlyPrayerCount.value = monthlyCount

                // Load prayer statistics by person
                val personStats = repository.getPrayerStatisticsByPerson(startDate, endDate)
                _prayerStatisticsByPerson.value = personStats

                // Load monthly answer rate
                val monthlyAnswer = repository.getMonthlyAnswerRate(startDate, endDate)
                _monthlyAnswerRate.value = monthlyAnswer

                _uiState.value = if (totalCount == 0) {
                    StatisticsUiState.Empty
                } else {
                    StatisticsUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = StatisticsUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun getDateRange(period: StatisticsPeriod): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        when (period) {
            StatisticsPeriod.ONE_MONTH -> calendar.add(Calendar.MONTH, -1)
            StatisticsPeriod.THREE_MONTHS -> calendar.add(Calendar.MONTH, -3)
            StatisticsPeriod.SIX_MONTHS -> calendar.add(Calendar.MONTH, -6)
            StatisticsPeriod.ONE_YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        val startDate = calendar.time
        return Pair(startDate, endDate)
    }
}

enum class StatisticsPeriod {
    ONE_MONTH,
    THREE_MONTHS,
    SIX_MONTHS,
    ONE_YEAR
}

sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    object Empty : StatisticsUiState()
    object Success : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}
