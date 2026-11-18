package com.example.domain

import com.example.data.local.ParsingRuleDao
import com.example.data.local.SettingsDao
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.local.VerificationStatus
import java.security.MessageDigest

sealed interface IngestionResult {
    data class Saved(val transaction: TransactionEntity) : IngestionResult
    data class Duplicate(val existingId: Long) : IngestionResult
    data class Ignored(val reason: String) : IngestionResult
}

class TransactionIngestionPipeline(
    private val parser: SmartMoneyNotificationParser,
    private val contactMatcher: ContactMatcher,
    private val categorizationEngine: CategorizationEngine,
    private val transactionDao: TransactionDao,
    private val settingsDao: SettingsDao,
    private val parsingRuleDao: ParsingRuleDao
) {
    suspend fun ingestNotification(
        packageName: String,
        title: String,
        body: String,
        timestamp: Long,
        bypassMonitoringToggle: Boolean = false,
        resolvedAppLabel: String? = null
    ): IngestionResult {
        val settings = settingsDao.getSettingsOnce()
        if (!bypassMonitoringToggle && settings?.monitoringEnabled == false) {
            return IngestionResult.Ignored("Transaction monitoring is paused by user")
        }

        val rules = parsingRuleDao.getAllParsingRulesOnce()
        val outcome = parser.parse(
            packageName = packageName,
            title = title,
            body = body,
            timestamp = timestamp,
            rules = rules,
            flagUnknownSenderAsPending = settings?.flagUnknownSenderAsPending ?: true,
            trackAllInstalledPaymentApps = settings?.trackAllInstalledPaymentApps ?: true,
            resolvedAppLabel = resolvedAppLabel
        )

        val parsed = when (outcome) {
            is ParseOutcome.Ignored -> return IngestionResult.Ignored(outcome.reason)
            is ParseOutcome.Parsed -> outcome.data
        }

        // Duplicate prevention:
        // 1) Check exact non-Unknown UTR / transaction reference
        if (parsed.transactionReference != TransactionEntity.UNKNOWN) {
            val existingByRef = transactionDao.findByReference(parsed.transactionReference)
            if (existingByRef != null) {
                return IngestionResult.Duplicate(existingByRef.id)
            }
        }

        // 2) Reliable composite dedupHash (type + amount + counterparty + package + 2-minute window)
        val dedupHash = computeDedupHash(
            type = parsed.type.name,
            amount = parsed.amount,
            personOrUpi = if (parsed.upiId != TransactionEntity.UNKNOWN) parsed.upiId else parsed.personName,
            packageName = parsed.paymentAppPackage,
            timestamp = parsed.timestamp,
            reference = parsed.transactionReference
        )
        val existingByHash = transactionDao.findByDedupHash(dedupHash)
        if (existingByHash != null) {
            return IngestionResult.Duplicate(existingByHash.id)
        }

        // Automatic Contact Identification
        val contactMatch = contactMatcher.matchOrCreate(
            personName = parsed.personName,
            phoneNumber = parsed.phoneNumber,
            upiId = parsed.upiId,
            timestamp = parsed.timestamp
        )

        // Automatic Expense Categorization
        val autoCategorize = settings?.autoCategorizeEnabled ?: true
        val category = if (autoCategorize) {
            categorizationEngine.resolveCategory(
                type = parsed.type,
                contact = contactMatch.contact,
                personName = contactMatch.resolvedName,
                upiId = contactMatch.resolvedUpiId,
                rawText = parsed.rawText,
                notes = ""
            )
        } else {
            categorizationEngine.resolveCategory(
                type = parsed.type,
                contact = null,
                personName = TransactionEntity.UNKNOWN,
                upiId = TransactionEntity.UNKNOWN,
                rawText = "",
                notes = ""
            )
        }

        val status = if (parsed.needsVerification) {
            VerificationStatus.PENDING
        } else {
            VerificationStatus.VERIFIED
        }

        val entity = TransactionEntity(
            type = parsed.type,
            amount = parsed.amount,
            personName = contactMatch.resolvedName,
            phoneNumber = contactMatch.resolvedPhone,
            upiId = contactMatch.resolvedUpiId,
            timestamp = parsed.timestamp,
            categoryId = category.id,
            categoryName = category.name,
            paymentApp = parsed.paymentApp,
            paymentAppPackage = parsed.paymentAppPackage,
            transactionReference = parsed.transactionReference,
            notes = if (parsed.uncertainReasons.isNotEmpty()) {
                "Review note: ${parsed.uncertainReasons.joinToString("; ")}"
            } else "",
            rawNotificationText = parsed.rawText,
            verificationStatus = status,
            contactId = contactMatch.contact?.id,
            dedupHash = dedupHash
        )

        val insertedId = transactionDao.insert(entity)
        return IngestionResult.Saved(entity.copy(id = insertedId))
    }

    companion object {
        fun computeDedupHash(
            type: String,
            amount: Double,
            personOrUpi: String,
            packageName: String,
            timestamp: Long,
            reference: String
        ): String {
            val bucket = timestamp / 120_000L // 2-minute deduplication bucket
            val raw = "$type|${String.format("%.2f", amount)}|${personOrUpi.lowercase()}|${packageName.lowercase()}|$reference|$bucket"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(32)
        }
    }
}
