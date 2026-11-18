package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ContactLedgerSummary(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val upiId: String,
    val notes: String,
    val defaultCategoryId: Long?,
    val totalSent: Double,
    val totalReceived: Double,
    val transactionCount: Int,
    val lastTransactionTime: Long?
) {
    val netSentMinusReceived: Double get() = totalSent - totalReceived
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE verificationStatus = 'PENDING' ORDER BY timestamp DESC")
    fun observePendingTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE verificationStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun observeTransactionsByContact(contactId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transactionReference = :ref AND transactionReference != 'Unknown' LIMIT 1")
    suspend fun findByReference(ref: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE dedupHash = :hash LIMIT 1")
    suspend fun findByDedupHash(hash: String): TransactionEntity?

    @Query(
        """
        SELECT categoryId FROM transactions 
        WHERE (contactId = :contactId AND :contactId IS NOT NULL)
           OR (upiId = :upiId AND upiId != 'Unknown')
        GROUP BY categoryId 
        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC 
        LIMIT 1
        """
    )
    suspend fun findMostFrequentCategoryForCounterparty(contactId: Long?, upiId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET personName = :personName, phoneNumber = :phoneNumber, upiId = :upiId WHERE contactId = :contactId")
    suspend fun syncContactDetailsToTransactions(
        contactId: Long,
        personName: String,
        phoneNumber: String,
        upiId: String
    )

    @Query("UPDATE transactions SET categoryName = :categoryName WHERE categoryId = :categoryId")
    suspend fun syncCategoryName(categoryId: Long, categoryName: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY updatedAt DESC, name ASC")
    fun observeAllContacts(): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT 
            c.id AS id,
            c.name AS name,
            c.phoneNumber AS phoneNumber,
            c.upiId AS upiId,
            c.notes AS notes,
            c.defaultCategoryId AS defaultCategoryId,
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0.0) AS totalSent,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0.0) AS totalReceived,
            COUNT(t.id) AS transactionCount,
            MAX(t.timestamp) AS lastTransactionTime
        FROM contacts c
        LEFT JOIN transactions t ON t.contactId = c.id
        GROUP BY c.id
        ORDER BY COALESCE(MAX(t.timestamp), c.updatedAt) DESC
        """
    )
    fun observeContactSummaries(): Flow<List<ContactLedgerSummary>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    fun observeContactById(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE LOWER(upiId) = LOWER(:upiId) AND upiId != 'Unknown' LIMIT 1")
    suspend fun findByUpiId(upiId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone AND phoneNumber != 'Unknown' LIMIT 1")
    suspend fun findByPhone(phone: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE LOWER(name) = LOWER(:name) AND name != 'Unknown' LIMIT 1")
    suspend fun findByName(name: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isCustom ASC, id ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isCustom ASC, id ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules ORDER BY priority DESC, id ASC")
    fun observeAllRules(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE isEnabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getEnabledRulesOnce(): List<CategoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<CategoryRuleEntity>)

    @Update
    suspend fun update(rule: CategoryRuleEntity)

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}

@Dao
interface ParsingRuleDao {
    @Query("SELECT * FROM parsing_rules ORDER BY id ASC")
    fun observeAllParsingRules(): Flow<List<ParsingRuleEntity>>

    @Query("SELECT * FROM parsing_rules ORDER BY id ASC")
    suspend fun getAllParsingRulesOnce(): List<ParsingRuleEntity>

    @Query("SELECT * FROM parsing_rules WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun getEnabledParsingRulesOnce(): List<ParsingRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ParsingRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<ParsingRuleEntity>)

    @Update
    suspend fun update(rule: ParsingRuleEntity)

    @Delete
    suspend fun delete(rule: ParsingRuleEntity)
}
