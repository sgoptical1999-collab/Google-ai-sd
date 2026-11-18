package com.example.domain

import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryRuleDao
import com.example.data.local.ContactEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionType

class CategorizationEngine(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val transactionDao: TransactionDao
) {
    suspend fun resolveCategory(
        type: TransactionType,
        contact: ContactEntity?,
        personName: String,
        upiId: String,
        rawText: String,
        notes: String
    ): CategoryEntity {
        val categories = categoryDao.getAllCategoriesOnce()
        val byId = categories.associateBy { it.id }

        // 1. Saved contact's explicit default category
        contact?.defaultCategoryId?.let { defaultCatId ->
            byId[defaultCatId]?.let { return it }
        }

        // 2. Editable keyword / UPI / contact rules
        val rules = categoryRuleDao.getEnabledRulesOnce()
        val haystack = "$personName $upiId $rawText $notes".lowercase()
        for (rule in rules) {
            val kw = rule.keyword.trim().lowercase()
            if (kw.isNotBlank() && haystack.contains(kw)) {
                byId[rule.categoryId]?.let { return it }
            }
        }

        // 3. Historical confirmed category for the same contact or UPI ID
        val historicalCategoryId = transactionDao.findMostFrequentCategoryForCounterparty(
            contactId = contact?.id,
            upiId = upiId
        )
        if (historicalCategoryId != null) {
            byId[historicalCategoryId]?.let { return it }
        }

        // 4. Fallback based on Income vs Expense
        val fallbackName = if (type == TransactionType.INCOME) "Other Income" else "Other Expenses"
        return categories.firstOrNull { it.name.equals(fallbackName, ignoreCase = true) }
            ?: categories.firstOrNull()
            ?: CategoryEntity(id = 16, name = fallbackName)
    }
}
