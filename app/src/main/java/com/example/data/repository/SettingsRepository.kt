package com.example.data.repository

import com.example.data.local.AppSettingsEntity
import com.example.data.local.ParsingRuleDao
import com.example.data.local.ParsingRuleEntity
import com.example.data.local.SettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDao: SettingsDao,
    private val parsingRuleDao: ParsingRuleDao
) : CoroutineRepository() {

    fun observeSettings(): Flow<AppSettingsEntity> =
        settingsDao.observeSettings().map { it ?: AppSettingsEntity() }

    fun observeParsingRules(): Flow<List<ParsingRuleEntity>> =
        parsingRuleDao.observeAllParsingRules()

    suspend fun getSettings(): AppSettingsEntity = executeIo {
        settingsDao.getSettingsOnce() ?: AppSettingsEntity()
    }

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) = executeIo {
        val current = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
        settingsDao.upsert(transform(current))
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        updateSettings { it.copy(monitoringEnabled = enabled) }
    }

    suspend fun setOpeningBalance(balance: Double) {
        updateSettings { it.copy(openingBalance = balance) }
    }

    suspend fun saveParsingRule(rule: ParsingRuleEntity): Long = executeIo {
        parsingRuleDao.insert(rule)
    }

    suspend fun updateParsingRule(rule: ParsingRuleEntity) = executeIo {
        parsingRuleDao.update(rule)
    }

    suspend fun deleteParsingRule(rule: ParsingRuleEntity) = executeIo {
        parsingRuleDao.delete(rule)
    }
}
