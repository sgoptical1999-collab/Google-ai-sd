package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.CategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.DateTimeUtils
import com.example.domain.FinancialReport
import com.example.domain.ReportGenerator
import com.example.domain.ReportPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val availableBalance: Double = 0.0,
    val totalMoneySent: Double = 0.0,
    val totalMoneyReceived: Double = 0.0,
    val pendingCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val weeklyReport: FinancialReport? = null,
    val monthlyReport: FinancialReport? = null,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val isLoaded: Boolean = false
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val reportGenerator: ReportGenerator
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeAll(),
        transactionRepository.observePendingCount(),
        categoryRepository.observeAll(),
        settingsRepository.observeSettings()
    ) { transactions, pendingCount, categories, settings ->
        val now = System.currentTimeMillis()
        val startToday = DateTimeUtils.startOfTodayMillis(now)
        val startMonth = DateTimeUtils.startOfMonthMillis(now)

        val todayTxns = transactions.filter { it.timestamp >= startToday }
        val monthTxns = transactions.filter { it.timestamp >= startMonth }

        val totalReceived = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalSent = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        DashboardUiState(
            todayIncome = todayTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            todayExpense = todayTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            monthlyIncome = monthTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            monthlyExpense = monthTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            availableBalance = settings.openingBalance + totalReceived - totalSent,
            totalMoneySent = totalSent,
            totalMoneyReceived = totalReceived,
            pendingCount = pendingCount,
            recentTransactions = transactions.take(8),
            categories = categories,
            weeklyReport = reportGenerator.generate(transactions, ReportPeriod.WEEKLY, now),
            monthlyReport = reportGenerator.generate(transactions, ReportPeriod.MONTHLY, now),
            settings = settings,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun toggleMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonitoringEnabled(enabled)
        }
    }

    fun addManualTransaction(
        type: TransactionType,
        amount: Double,
        personName: String,
        phoneNumber: String,
        upiId: String,
        categoryId: Long,
        categoryName: String,
        paymentApp: String,
        reference: String,
        notes: String
    ) {
        viewModelScope.launch {
            transactionRepository.saveManualTransaction(
                type = type,
                amount = amount,
                personName = personName,
                phoneNumber = phoneNumber,
                upiId = upiId,
                timestamp = System.currentTimeMillis(),
                categoryId = categoryId,
                categoryName = categoryName,
                paymentApp = paymentApp,
                reference = reference,
                notes = notes
            )
        }
    }
}
