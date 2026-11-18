package com.example.domain

import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.TransactionEntity

data class ContactMatchResult(
    val contact: ContactEntity?,
    val resolvedName: String,
    val resolvedPhone: String,
    val resolvedUpiId: String
)

class ContactMatcher(private val contactDao: ContactDao) {

    suspend fun matchOrCreate(
        personName: String,
        phoneNumber: String,
        upiId: String,
        timestamp: Long
    ): ContactMatchResult {
        val cleanName = personName.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanPhone = phoneNumber.trim().ifBlank { TransactionEntity.UNKNOWN }
        val cleanUpi = upiId.trim().ifBlank { TransactionEntity.UNKNOWN }

        val hasUpi = cleanUpi != TransactionEntity.UNKNOWN
        val hasPhone = cleanPhone != TransactionEntity.UNKNOWN
        val hasName = cleanName != TransactionEntity.UNKNOWN

        // If nothing identifiable is present, do not create a dummy "Unknown" contact
        if (!hasUpi && !hasPhone && !hasName) {
            return ContactMatchResult(
                contact = null,
                resolvedName = TransactionEntity.UNKNOWN,
                resolvedPhone = TransactionEntity.UNKNOWN,
                resolvedUpiId = TransactionEntity.UNKNOWN
            )
        }

        // Priority matching: 1. UPI ID -> 2. Phone Number -> 3. Exact Name
        var existing: ContactEntity? = null
        if (hasUpi) {
            existing = contactDao.findByUpiId(cleanUpi)
        }
        if (existing == null && hasPhone) {
            existing = contactDao.findByPhone(cleanPhone)
        }
        if (existing == null && hasName) {
            existing = contactDao.findByName(cleanName)
        }

        if (existing != null) {
            // Enrich missing fields without inventing anything
            val updatedName = if (existing.name == TransactionEntity.UNKNOWN && hasName) cleanName else existing.name
            val updatedPhone = if (existing.phoneNumber == TransactionEntity.UNKNOWN && hasPhone) cleanPhone else existing.phoneNumber
            val updatedUpi = if (existing.upiId == TransactionEntity.UNKNOWN && hasUpi) cleanUpi else existing.upiId

            val updated = existing.copy(
                name = updatedName,
                phoneNumber = updatedPhone,
                upiId = updatedUpi,
                updatedAt = timestamp
            )
            if (updated != existing) {
                contactDao.update(updated)
            }
            return ContactMatchResult(
                contact = updated,
                resolvedName = updated.name,
                resolvedPhone = updated.phoneNumber,
                resolvedUpiId = updated.upiId
            )
        }

        // Create a new contact record
        val displayName = when {
            hasName -> cleanName
            hasUpi -> cleanUpi.substringBefore("@").replace(".", " ").replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                .ifBlank { cleanUpi }
            else -> cleanPhone
        }

        val newContact = ContactEntity(
            name = displayName,
            phoneNumber = cleanPhone,
            upiId = cleanUpi,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val newId = contactDao.insert(newContact)
        val saved = newContact.copy(id = newId)

        return ContactMatchResult(
            contact = saved,
            resolvedName = saved.name,
            resolvedPhone = saved.phoneNumber,
            resolvedUpiId = saved.upiId
        )
    }
}
