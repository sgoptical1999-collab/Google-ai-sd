package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.DatabaseSeeder
import com.example.data.local.SmartMoneyDatabase
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ContactRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.CategorizationEngine
import com.example.domain.ContactMatcher
import com.example.domain.ReportGenerator
import com.example.domain.SmartMoneyNotificationParser
import com.example.domain.TransactionIngestionPipeline
import com.example.ui.viewmodel.ContactsViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.ReportsViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TransactionsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Core manual dependency injection container providing singletons for
 * Room database, DAOs, domain engines, and repositories.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: SmartMoneyDatabase by lazy {
        SmartMoneyDatabase.create(appContext)
    }

    val notificationParser: SmartMoneyNotificationParser by lazy {
        SmartMoneyNotificationParser()
    }

    val contactMatcher: ContactMatcher by lazy {
        ContactMatcher(database.contactDao())
    }

    val categorizationEngine: CategorizationEngine by lazy {
        CategorizationEngine(
            categoryDao = database.categoryDao(),
            categoryRuleDao = database.categoryRuleDao(),
            transactionDao = database.transactionDao()
        )
    }

    val ingestionPipeline: TransactionIngestionPipeline by lazy {
        TransactionIngestionPipeline(
            parser = notificationParser,
            contactMatcher = contactMatcher,
            categorizationEngine = categorizationEngine,
            transactionDao = database.transactionDao(),
            settingsDao = database.settingsDao(),
            parsingRuleDao = database.parsingRuleDao()
        )
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(
            transactionDao = database.transactionDao(),
            contactDao = database.contactDao(),
            categoryDao = database.categoryDao(),
            contactMatcher = contactMatcher,
            ingestionPipeline = ingestionPipeline
        )
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepository(
            contactDao = database.contactDao(),
            transactionDao = database.transactionDao()
        )
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(
            categoryDao = database.categoryDao(),
            categoryRuleDao = database.categoryRuleDao(),
            transactionDao = database.transactionDao()
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            settingsDao = database.settingsDao(),
            parsingRuleDao = database.parsingRuleDao()
        )
    }

    val reportGenerator: ReportGenerator by lazy {
        ReportGenerator()
    }

    init {
        appScope.launch {
            runCatching { DatabaseSeeder.ensureSeeded(database) }
        }
    }
}

class SmartMoneyViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    settingsRepository = container.settingsRepository,
                    reportGenerator = container.reportGenerator
                ) as T

            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    settingsRepository = container.settingsRepository
                ) as T

            modelClass.isAssignableFrom(ContactsViewModel::class.java) ->
                ContactsViewModel(
                    contactRepository = container.contactRepository,
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    settingsRepository = container.settingsRepository
                ) as T

            modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
                ReportsViewModel(
                    transactionRepository = container.transactionRepository,
                    settingsRepository = container.settingsRepository,
                    reportGenerator = container.reportGenerator
                ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    categoryRepository = container.categoryRepository,
                    transactionRepository = container.transactionRepository
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
