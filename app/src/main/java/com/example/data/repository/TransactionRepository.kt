package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.ContactDao
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.VerificationStatus
import com.example.domain.ContactMatcher
import com.example.domain.IngestionResult
import com.example.domain.TransactionIngestionPipeline
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val contactDao: ContactDao,
    private val categoryDao: CategoryDao,
    private val contactMatcher: ContactMatcher,
    private val ingestionPipeline: TransactionIngestionPipeline
) : CoroutineRepository(), BaseRepository<TransactionEntity, Long> {

    override fun observeAll(): Flow<List<TransactionEntity>> =
        transactionDao.observeAllTransactions()

    fun observePending(): Flow<List<TransactionEntity>> =
        transactionDao.observePendingTransactions()

    fun observePendingCount(): Flow<Int> =
        transactionDao.observePendingCount()

    fun observeByContact(contactId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeTransactionsByContact(contactId)

    override suspend fun save(item: TransactionEntity): Long = executeIo {
        val category = categoryDao.getById(item.categoryId)
        val contactMatch = contactMatcher.matchOrCreate(
            personName = item.personName,
            phoneNumber = item.phoneNumber,
            upiId = item.upiId,
            timestamp = item.timestamp
        )

        val cleanRef = item.transactionReference.trim().ifBlank { TransactionEntity.UNKNOWN }
        val dedupHash = TransactionIngestionPipeline.computeDedupHash(
            type = item.type.name,
            amount = item.amount,
            personOrUpi = if (contactMatch.resolvedUpiId != TransactionEntity.UNKNOWN) {
                contactMatch.resolvedUpiId
            } else {
                contactMatch.resolvedName
            },
            packageName = item.paymentAppPackage.ifBlank { item.paymentApp },
            timestamp = item.timestamp,
            reference = cleanRef
        )

        val toSave = item.copy(
            personName = contactMatch.resolvedName,
            phoneNumber = contactMatch.resolvedPhone,
            upiId = contactMatch.resolvedUpiId,
            categoryName = category?.name ?: item.categoryName,
            transactionReference = cleanRef,
            contactId = contactMatch.contact?.id ?: item.contactId,
            dedupHash = dedupHash
        )
        transactionDao.insert(toSave)
    }

    override suspend fun update(item: TransactionEntity) = executeIo {
        val category = categoryDao.getById(item.categoryId)
        val contactMatch = contactMatcher.matchOrCreate(
            personName = item.personName,
            phoneNumber = item.phoneNumber,
            upiId = item.upiId,
            timestamp = item.timestamp
        )
        val updated = item.copy(
            personName = contactMatch.resolvedName,
            phoneNumber = contactMatch.resolvedPhone,
            upiId = contactMatch.resolvedUpiId,
            categoryName = category?.name ?: item.categoryName,
            contactId = contactMatch.contact?.id ?: item.contactId
        )
        transactionDao.update(updated)
    }

    suspend fun confirmAndLearn(
        transaction: TransactionEntity,
        rememberCategoryForContact: Boolean = true
    ) = executeIo {
        val confirmed = transaction.copy(verificationStatus = VerificationStatus.VERIFIED)
        update(confirmed)
        if (rememberCategoryForContact && confirmed.contactId != null) {
            contactDao.getById(confirmed.contactId)?.let { contact ->
                contactDao.update(contact.copy(defaultCategoryId = confirmed.categoryId))
            }
        }
    }

    override suspend fun delete(item: TransactionEntity) = executeIo {
        transactionDao.delete(item)
    }

    suspend fun deleteById(id: Long) = executeIo {
        transactionDao.deleteById(id)
    }

    suspend fun ingestRawNotification(
        packageName: String,
        title: String,
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        bypassMonitoringToggle: Boolean = false,
        resolvedAppLabel: String? = null
    ): IngestionResult = executeIo {
        ingestionPipeline.ingestNotification(
            packageName = packageName,
            title = title,
            body = body,
            timestamp = timestamp,
            bypassMonitoringToggle = bypassMonitoringToggle,
            resolvedAppLabel = resolvedAppLabel
        )
    }

    suspend fun saveManualTransaction(
        type: TransactionType,
        amount: Double,
        personName: String,
        phoneNumber: String,
        upiId: String,
        timestamp: Long,
        categoryId: Long,
        categoryName: String,
        paymentApp: String,
        reference: String,
        notes: String
    ): Long {
        val cleanName = personName.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanPhone = phoneNumber.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanUpi = upiId.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanRef = reference.trim().ifBlank { TransactionEntity.UNKNOWN }
        return save(
            TransactionEntity(
                type = type,
                amount = amount,
                personName = cleanName,
                phoneNumber = cleanPhone,
                upiId = cleanUpi,
                timestamp = timestamp,
                categoryId = categoryId,
                categoryName = categoryName,
                paymentApp = paymentApp.trim().ifBlank { "Manual Entry" },
                paymentAppPackage = "manual",
                transactionReference = cleanRef,
                notes = notes.trim(),
                verificationStatus = VerificationStatus.VERIFIED
            )
        )
    }
}
