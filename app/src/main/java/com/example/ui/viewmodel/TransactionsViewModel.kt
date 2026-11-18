package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.VerificationStatus
import com.example.data.repository.CategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Range")
}

enum class VerificationTab(val label: String) {
    ALL("All"),
    PENDING("Pending Review"),
    VERIFIED("Verified")
}

data class TransactionFilterState(
    val query: String = "",
    val selectedTab: VerificationTab = VerificationTab.ALL,
    val typeFilter: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val minAmount: String = "",
    val maxAmount: String = ""
)

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val pendingCount: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val filter: TransactionFilterState = TransactionFilterState(),
    val settings: AppSettingsEntity = AppSettingsEntity()
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(TransactionFilterState())

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        settingsRepository.observeSettings(),
        filterState
    ) { allTxns, categories, settings, filter ->
        val pendingCount = allTxns.count { it.verificationStatus == VerificationStatus.PENDING }
        val filtered = applyFilters(allTxns, filter)
        TransactionsUiState(
            transactions = filtered,
            pendingCount = pendingCount,
            categories = categories,
            filter = filter,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState()
    )

    private fun applyFilters(
        list: List<TransactionEntity>,
        filter: TransactionFilterState
    ): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val q = filter.query.trim().lowercase()
        val minAmt = filter.minAmount.toDoubleOrNull()
        val maxAmt = filter.maxAmount.toDoubleOrNull()

        return list.filter { t ->
            val tabMatches = when (filter.selectedTab) {
                VerificationTab.ALL -> true
                VerificationTab.PENDING -> t.verificationStatus == VerificationStatus.PENDING
                VerificationTab.VERIFIED -> t.verificationStatus != VerificationStatus.PENDING
            }
            if (!tabMatches) return@filter false

            if (filter.typeFilter != null && t.type != filter.typeFilter) return@filter false
            if (filter.selectedCategoryId != null && t.categoryId != filter.selectedCategoryId) return@filter false

            val dateMatches = when (filter.dateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> t.timestamp >= DateTimeUtils.startOfTodayMillis(now)
                DateRangeFilter.THIS_WEEK -> t.timestamp >= DateTimeUtils.startOfWeekMillis(now)
                DateRangeFilter.THIS_MONTH -> t.timestamp >= DateTimeUtils.startOfMonthMillis(now)
                DateRangeFilter.CUSTOM -> {
                    val afterStart = filter.customStartMillis?.let { t.timestamp >= it } ?: true
                    val beforeEnd = filter.customEndMillis?.let { t.timestamp <= it + 86_399_999L } ?: true
                    afterStart && beforeEnd
                }
            }
            if (!dateMatches) return@filter false

            if (minAmt != null && t.amount < minAmt) return@filter false
            if (maxAmt != null && t.amount > maxAmt) return@filter false

            if (q.isNotBlank()) {
                val matchesQuery = t.personName.lowercase().contains(q) ||
                    t.phoneNumber.lowercase().contains(q) ||
                    t.upiId.lowercase().contains(q) ||
                    t.categoryName.lowercase().contains(q) ||
                    t.paymentApp.lowercase().contains(q) ||
                    t.transactionReference.lowercase().contains(q) ||
                    t.notes.lowercase().contains(q)
                if (!matchesQuery) return@filter false
            }

            true
        }
    }

    fun updateQuery(query: String) {
        filterState.update { it.copy(query = query) }
    }

    fun selectTab(tab: VerificationTab) {
        filterState.update { it.copy(selectedTab = tab) }
    }

    fun updateFilter(transform: (TransactionFilterState) -> TransactionFilterState) {
        filterState.update(transform)
    }

    fun resetFilters() {
        filterState.update { TransactionFilterState(selectedTab = it.selectedTab) }
    }

    fun saveOrUpdateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.id == 0L) {
                transactionRepository.save(transaction)
            } else {
                transactionRepository.update(transaction)
            }
        }
    }

    fun confirmTransaction(transaction: TransactionEntity, rememberCategory: Boolean = true) {
        viewModelScope.launch {
            transactionRepository.confirmAndLearn(transaction, rememberCategory)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }
}
