package com.example.data.repository

import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.ContactLedgerSummary
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

class ContactRepository(
    private val contactDao: ContactDao,
    private val transactionDao: TransactionDao
) : CoroutineRepository(), BaseRepository<ContactEntity, Long> {

    override fun observeAll(): Flow<List<ContactEntity>> = contactDao.observeAllContacts()

    fun observeSummaries(): Flow<List<ContactLedgerSummary>> = contactDao.observeContactSummaries()

    fun observeById(id: Long): Flow<ContactEntity?> = contactDao.observeContactById(id)

    override suspend fun save(item: ContactEntity): Long = executeIo {
        contactDao.insert(item)
    }

    override suspend fun update(item: ContactEntity) = executeIo {
        val cleanName = item.name.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanPhone = item.phoneNumber.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanUpi = item.upiId.trim().ifBlank { TransactionEntity.UNKNOWN }
        val updated = item.copy(
            name = cleanName,
            phoneNumber = cleanPhone,
            upiId = cleanUpi,
            updatedAt = System.currentTimeMillis()
        )
        contactDao.update(updated)
        transactionDao.syncContactDetailsToTransactions(
            contactId = updated.id,
            personName = updated.name,
            phoneNumber = updated.phoneNumber,
            upiId = updated.upiId
        )
    }

    override suspend fun delete(item: ContactEntity) = executeIo {
        contactDao.delete(item)
    }
}
