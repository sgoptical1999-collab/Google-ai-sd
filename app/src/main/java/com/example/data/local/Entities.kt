package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class VerificationStatus {
    VERIFIED,
    PENDING,
    EDITED
}

enum class CategoryScope {
    INCOME,
    EXPENSE,
    BOTH
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dedupHash"]),
        Index(value = ["transactionReference"]),
        Index(value = ["timestamp"]),
        Index(value = ["contactId"]),
        Index(value = ["categoryId"]),
        Index(value = ["verificationStatus"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amount: Double,
    val personName: String = UNKNOWN,
    val phoneNumber: String = UNKNOWN,
    val upiId: String = UNKNOWN,
    val timestamp: Long,
    val categoryId: Long,
    val categoryName: String,
    val paymentApp: String = UNKNOWN,
    val paymentAppPackage: String = "",
    val transactionReference: String = UNKNOWN,
    val notes: String = "",
    val rawNotificationText: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val contactId: Long? = null,
    val dedupHash: String = ""
) {
    companion object {
        const val UNKNOWN = "Unknown"
    }
}

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["upiId"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["name"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phoneNumber: String = TransactionEntity.UNKNOWN,
    val upiId: String = TransactionEntity.UNKNOWN,
    val notes: String = "",
    val defaultCategoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val scope: CategoryScope = CategoryScope.EXPENSE,
    val iconKey: String = "category",
    val colorHex: Long = 0xFF10B981,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "category_rules",
    indices = [Index(value = ["categoryId"])]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val keyword: String,
    val categoryId: Long,
    val matchType: String = "KEYWORD", // KEYWORD, UPI_ID, CONTACT_NAME
    val priority: Int = 10,
    val isEnabled: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val monitoringEnabled: Boolean = true,
    val trackAllInstalledPaymentApps: Boolean = true,
    val autoCategorizeEnabled: Boolean = true,
    val flagUnknownSenderAsPending: Boolean = true,
    val openingBalance: Double = 0.0,
    val currencySymbol: String = "₹",
    val businessName: String = "Personal & Business"
)

@Entity(
    tableName = "parsing_rules",
    indices = [Index(value = ["packageName"])]
)
data class ParsingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appDisplayName: String,
    val packageName: String,
    val amountRegex: String,
    val sentKeywords: String, // comma-separated
    val receivedKeywords: String, // comma-separated
    val referenceRegex: String,
    val upiIdRegex: String,
    val phoneRegex: String,
    val senderNameRegex: String,
    val receiverNameRegex: String,
    val isEnabled: Boolean = true
)

class SmartMoneyTypeConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromVerificationStatus(value: VerificationStatus): String = value.name

    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus =
        runCatching { VerificationStatus.valueOf(value) }.getOrDefault(VerificationStatus.PENDING)

    @TypeConverter
    fun fromCategoryScope(value: CategoryScope): String = value.name

    @TypeConverter
    fun toCategoryScope(value: String): CategoryScope =
        runCatching { CategoryScope.valueOf(value) }.getOrDefault(CategoryScope.EXPENSE)
}
