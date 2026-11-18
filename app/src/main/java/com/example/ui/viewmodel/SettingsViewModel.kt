package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryRuleEntity
import com.example.data.local.CategoryScope
import com.example.data.local.ParsingRuleEntity
import com.example.data.repository.CategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.IngestionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val parsingRules: List<ParsingRuleEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val categoryRules: List<CategoryRuleEntity> = emptyList(),
    val parserTestStatus: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val parserTestStatus = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        settingsRepository.observeParsingRules(),
        categoryRepository.observeAll(),
        categoryRepository.observeAllRules(),
        parserTestStatus
    ) { settings, parsingRules, categories, catRules, testStatus ->
        SettingsUiState(
            settings = settings,
            parsingRules = parsingRules,
            categories = categories,
            categoryRules = catRules,
            parserTestStatus = testStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonitoringEnabled(enabled) }
    }

    fun setTrackAllInstalledPaymentApps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(trackAllInstalledPaymentApps = enabled) }
        }
    }

    fun setAutoCategorizeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoCategorizeEnabled = enabled) }
        }
    }

    fun setFlagUnknownAsPending(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(flagUnknownSenderAsPending = enabled) }
        }
    }

    fun updateOpeningBalance(balance: Double) {
        viewModelScope.launch { settingsRepository.setOpeningBalance(balance) }
    }

    fun updateBusinessName(name: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(businessName = name.trim().ifBlank { "Personal & Business" })
            }
        }
    }

    fun saveParsingRule(rule: ParsingRuleEntity) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                settingsRepository.saveParsingRule(rule)
            } else {
                settingsRepository.updateParsingRule(rule)
            }
        }
    }

    fun deleteParsingRule(rule: ParsingRuleEntity) {
        viewModelScope.launch { settingsRepository.deleteParsingRule(rule) }
    }

    fun createCategory(name: String, scope: CategoryScope, colorHex: Long) {
        viewModelScope.launch {
            categoryRepository.createCustomCategory(name, scope, colorHex)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }

    fun saveCategoryRule(keyword: String, categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.saveRule(
                CategoryRuleEntity(
                    keyword = keyword.trim(),
                    categoryId = categoryId,
                    priority = 25
                )
            )
        }
    }

    fun deleteCategoryRule(rule: CategoryRuleEntity) {
        viewModelScope.launch { categoryRepository.deleteRule(rule) }
    }

    fun testNotificationParser(packageName: String, title: String, body: String) {
        viewModelScope.launch {
            when (
                val res = transactionRepository.ingestRawNotification(
                    packageName = packageName,
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    bypassMonitoringToggle = true
                )
            ) {
                is IngestionResult.Saved -> {
                    val t = res.transaction
                    parserTestStatus.value = "Captured ${t.type.name}: ₹${t.amount} • ${t.personName} (${t.categoryName}) [${t.verificationStatus.name}]"
                }
                is IngestionResult.Duplicate -> {
                    parserTestStatus.value = "Duplicate blocked (matches transaction #${res.existingId})"
                }
                is IngestionResult.Ignored -> {
                    parserTestStatus.value = "Ignored: ${res.reason}"
                }
            }
        }
    }

    fun clearParserTestStatus() {
        parserTestStatus.value = null
    }
}
