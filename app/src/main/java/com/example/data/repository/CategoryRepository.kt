package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryRuleDao
import com.example.data.local.CategoryRuleEntity
import com.example.data.local.CategoryScope
import com.example.data.local.TransactionDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val transactionDao: TransactionDao
) : CoroutineRepository(), BaseRepository<CategoryEntity, Long> {

    override fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAllCategories()

    fun observeAllRules(): Flow<List<CategoryRuleEntity>> = categoryRuleDao.observeAllRules()

    override suspend fun save(item: CategoryEntity): Long = executeIo {
        categoryDao.insert(item)
    }

    suspend fun createCustomCategory(
        name: String,
        scope: CategoryScope,
        colorHex: Long = 0xFF10B981
    ): Long = executeIo {
        val cleanName = name.trim()
        val existing = categoryDao.findByName(cleanName)
        if (existing != null) return@executeIo existing.id
        categoryDao.insert(
            CategoryEntity(
                name = cleanName,
                scope = scope,
                iconKey = "label",
                colorHex = colorHex,
                isCustom = true
            )
        )
    }

    override suspend fun update(item: CategoryEntity) = executeIo {
        categoryDao.update(item)
        transactionDao.syncCategoryName(item.id, item.name)
    }

    override suspend fun delete(item: CategoryEntity) = executeIo {
        if (item.isCustom) {
            categoryDao.delete(item)
        }
    }

    suspend fun saveRule(rule: CategoryRuleEntity): Long = executeIo {
        categoryRuleDao.insert(rule)
    }

    suspend fun updateRule(rule: CategoryRuleEntity) = executeIo {
        categoryRuleDao.update(rule)
    }

    suspend fun deleteRule(rule: CategoryRuleEntity) = executeIo {
        categoryRuleDao.delete(rule)
    }
}
