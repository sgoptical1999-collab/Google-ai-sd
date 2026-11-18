package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ContactEntity
import com.example.data.local.ContactLedgerSummary
import com.example.data.local.TransactionEntity
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ContactRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<ContactLedgerSummary> = emptyList(),
    val searchQuery: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity()
)

data class ContactDetailUiState(
    val contact: ContactEntity? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val totalSent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val categories: List<CategoryEntity> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity()
) {
    val netBalance: Double get() = totalSent - totalReceived
}

class ContactsViewModel(
    private val contactRepository: ContactRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.observeSummaries(),
        searchQuery,
        categoryRepository.observeAll(),
        settingsRepository.observeSettings()
    ) { summaries, query, categories, settings ->
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            summaries
        } else {
            summaries.filter {
                it.name.lowercase().contains(q) ||
                    it.upiId.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q)
            }
        }
        ContactsUiState(
            contacts = filtered,
            searchQuery = query,
            categories = categories,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState()
    )

    fun observeContactDetail(contactId: Long): StateFlow<ContactDetailUiState> = combine(
        contactRepository.observeById(contactId),
        transactionRepository.observeByContact(contactId),
        categoryRepository.observeAll(),
        settingsRepository.observeSettings()
    ) { contact, txns, categories, settings ->
        val sent = txns.filter { it.type == com.example.data.local.TransactionType.EXPENSE }.sumOf { it.amount }
        val rec = txns.filter { it.type == com.example.data.local.TransactionType.INCOME }.sumOf { it.amount }
        ContactDetailUiState(
            contact = contact,
            transactions = txns,
            totalSent = sent,
            totalReceived = rec,
            categories = categories,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactDetailUiState()
    )

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun updateContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.update(contact)
        }
    }

    fun createContact(name: String, phone: String, upiId: String, notes: String, defaultCatId: Long?) {
        viewModelScope.launch {
            contactRepository.save(
                ContactEntity(
                    name = name.trim(),
                    phoneNumber = phone.trim().ifBlank { TransactionEntity.UNKNOWN },
                    upiId = upiId.trim().ifBlank { TransactionEntity.UNKNOWN },
                    notes = notes.trim(),
                    defaultCategoryId = defaultCatId
                )
            )
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.delete(contact)
        }
    }
}
