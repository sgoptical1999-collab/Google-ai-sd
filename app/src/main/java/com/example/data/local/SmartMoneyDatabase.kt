package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        ContactEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        AppSettingsEntity::class,
        ParsingRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(SmartMoneyTypeConverters::class)
abstract class SmartMoneyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun contactDao(): ContactDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun settingsDao(): SettingsDao
    abstract fun parsingRuleDao(): ParsingRuleDao

    companion object {
        private const val DB_NAME = "smart_money_tracker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN trackAllInstalledPaymentApps INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun create(context: Context): SmartMoneyDatabase {
            lateinit var roomDb: SmartMoneyDatabase
            roomDb = Room.databaseBuilder(
                context.applicationContext,
                SmartMoneyDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            runCatching { DatabaseSeeder.seedDefaults(roomDb) }
                        }
                    }
                })
                .build()
            return roomDb
        }
    }
}

object DatabaseSeeder {
    private const val DEFAULT_AMOUNT_REGEX =
        """(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    private const val DEFAULT_REF_REGEX =
        """(?:UTR|UPI\s*Ref(?:\s*No)?|Ref(?:\s*No)?|Txn\s*ID|Transaction\s*ID|Ref\.?|RRN)\s*[:#\-]?\s*([A-Za-z0-9]{6,24})"""
    private const val DEFAULT_UPI_REGEX =
        """([a-zA-Z0-9._\-]{2,}@[a-zA-Z0-9]{2,})"""
    private const val DEFAULT_PHONE_REGEX =
        """(?:(?:Mob|Mobile|Phone|Ph|Tel|No\.?|Contact)\s*[:#\-]?\s*)((?:\+91[\s\-]?)?[6-9][0-9]{9})(?!@)"""
    private const val DEFAULT_SENDER_REGEX =
        """(?:from|by|sender)\s+([A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s*(?:,|—|\.|;|\n|$)|\s+(?:via|on|using|to|through|ref|utr|upi|a/c|acct|account|in|at|for|mob|mobile|phone)\b)"""
    private const val DEFAULT_RECEIVER_REGEX =
        """(?:to|paid\s+to|sent\s+to|transfer\s+to|at)\s+([A-Za-z][A-Za-z0-9 .&'\-]{1,40}?)(?=\s*(?:,|—|\.|;|\n|$)|\s+(?:via|on|using|from|through|ref|utr|upi|a/c|acct|account|in|for|mob|mobile|phone)\b)"""

    private const val SENT_KEYWORDS =
        "paid,sent,debited,spent,transferred,deducted,withdrawn,payment of,purchase,autopay"
    private const val RECEIVED_KEYWORDS =
        "received,credited,deposited,refund,cashback,settled,added to,got paid"

    suspend fun ensureSeeded(db: SmartMoneyDatabase) {
        seedDefaults(db)
    }

    suspend fun seedDefaults(db: SmartMoneyDatabase?, dbInstance: SmartMoneyDatabase? = db) {
        val targetDb = db ?: dbInstance ?: return
        val categoryDao = targetDb.categoryDao()
        val existingCategories = categoryDao.getAllCategoriesOnce()
        if (existingCategories.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(id = 1, name = "Personal Expenses", scope = CategoryScope.EXPENSE, iconKey = "person", colorHex = 0xFF38BDF8),
                CategoryEntity(id = 2, name = "Business Expenses", scope = CategoryScope.EXPENSE, iconKey = "business", colorHex = 0xFF6366F1),
                CategoryEntity(id = 3, name = "Food & Restaurant", scope = CategoryScope.EXPENSE, iconKey = "restaurant", colorHex = 0xFFF97316),
                CategoryEntity(id = 4, name = "Shopping", scope = CategoryScope.EXPENSE, iconKey = "shopping", colorHex = 0xFFEC4899),
                CategoryEntity(id = 5, name = "Grocery", scope = CategoryScope.EXPENSE, iconKey = "grocery", colorHex = 0xFF84CC16),
                CategoryEntity(id = 6, name = "Electricity", scope = CategoryScope.EXPENSE, iconKey = "bolt", colorHex = 0xFFEAB308),
                CategoryEntity(id = 7, name = "Mobile Recharge", scope = CategoryScope.EXPENSE, iconKey = "phone", colorHex = 0xFF06B6D4),
                CategoryEntity(id = 8, name = "Fuel & Transport", scope = CategoryScope.EXPENSE, iconKey = "fuel", colorHex = 0xFF14B8A6),
                CategoryEntity(id = 9, name = "Staff Salary", scope = CategoryScope.EXPENSE, iconKey = "badge", colorHex = 0xFF8B5CF6),
                CategoryEntity(id = 10, name = "Shop Rent", scope = CategoryScope.EXPENSE, iconKey = "store", colorHex = 0xFFF43F5E),
                CategoryEntity(id = 11, name = "Medical", scope = CategoryScope.EXPENSE, iconKey = "medical", colorHex = 0xFFEF4444),
                CategoryEntity(id = 12, name = "Loan Payment", scope = CategoryScope.EXPENSE, iconKey = "bank", colorHex = 0xFFA855F7),
                CategoryEntity(id = 13, name = "Supplier Payment", scope = CategoryScope.EXPENSE, iconKey = "inventory", colorHex = 0xFF0EA5E9),
                CategoryEntity(id = 14, name = "Customer Payment", scope = CategoryScope.INCOME, iconKey = "payments", colorHex = 0xFF10B981),
                CategoryEntity(id = 15, name = "Other Income", scope = CategoryScope.INCOME, iconKey = "trending_up", colorHex = 0xFF22C55E),
                CategoryEntity(id = 16, name = "Other Expenses", scope = CategoryScope.EXPENSE, iconKey = "receipt", colorHex = 0xFF64748B)
            )
            categoryDao.insertAll(defaultCategories)
        }

        val ruleDao = targetDb.categoryRuleDao()
        if (ruleDao.getEnabledRulesOnce().isEmpty()) {
            val rules = listOf(
                CategoryRuleEntity(keyword = "swiggy", categoryId = 3, priority = 20),
                CategoryRuleEntity(keyword = "zomato", categoryId = 3, priority = 20),
                CategoryRuleEntity(keyword = "restaurant", categoryId = 3, priority = 15),
                CategoryRuleEntity(keyword = "cafe", categoryId = 3, priority = 15),
                CategoryRuleEntity(keyword = "hotel", categoryId = 3, priority = 12),
                CategoryRuleEntity(keyword = "amazon", categoryId = 4, priority = 16),
                CategoryRuleEntity(keyword = "flipkart", categoryId = 4, priority = 16),
                CategoryRuleEntity(keyword = "myntra", categoryId = 4, priority = 16),
                CategoryRuleEntity(keyword = "blinkit", categoryId = 5, priority = 20),
                CategoryRuleEntity(keyword = "zepto", categoryId = 5, priority = 20),
                CategoryRuleEntity(keyword = "bigbasket", categoryId = 5, priority = 20),
                CategoryRuleEntity(keyword = "dmart", categoryId = 5, priority = 20),
                CategoryRuleEntity(keyword = "grocery", categoryId = 5, priority = 18),
                CategoryRuleEntity(keyword = "kirana", categoryId = 5, priority = 18),
                CategoryRuleEntity(keyword = "electricity", categoryId = 6, priority = 20),
                CategoryRuleEntity(keyword = "bescom", categoryId = 6, priority = 20),
                CategoryRuleEntity(keyword = "mahadiscom", categoryId = 6, priority = 20),
                CategoryRuleEntity(keyword = "power", categoryId = 6, priority = 12),
                CategoryRuleEntity(keyword = "jio", categoryId = 7, priority = 18),
                CategoryRuleEntity(keyword = "airtel", categoryId = 7, priority = 18),
                CategoryRuleEntity(keyword = "recharge", categoryId = 7, priority = 18),
                CategoryRuleEntity(keyword = "petrol", categoryId = 8, priority = 20),
                CategoryRuleEntity(keyword = "diesel", categoryId = 8, priority = 20),
                CategoryRuleEntity(keyword = "fuel", categoryId = 8, priority = 20),
                CategoryRuleEntity(keyword = "uber", categoryId = 8, priority = 18),
                CategoryRuleEntity(keyword = "ola", categoryId = 8, priority = 18),
                CategoryRuleEntity(keyword = "irctc", categoryId = 8, priority = 18),
                CategoryRuleEntity(keyword = "salary", categoryId = 9, priority = 20),
                CategoryRuleEntity(keyword = "wages", categoryId = 9, priority = 18),
                CategoryRuleEntity(keyword = "rent", categoryId = 10, priority = 18),
                CategoryRuleEntity(keyword = "pharmacy", categoryId = 11, priority = 20),
                CategoryRuleEntity(keyword = "medical", categoryId = 11, priority = 20),
                CategoryRuleEntity(keyword = "apollo", categoryId = 11, priority = 18),
                CategoryRuleEntity(keyword = "hospital", categoryId = 11, priority = 20),
                CategoryRuleEntity(keyword = "emi", categoryId = 12, priority = 20),
                CategoryRuleEntity(keyword = "loan", categoryId = 12, priority = 18),
                CategoryRuleEntity(keyword = "supplier", categoryId = 13, priority = 18),
                CategoryRuleEntity(keyword = "wholesale", categoryId = 13, priority = 18),
                CategoryRuleEntity(keyword = "invoice", categoryId = 14, priority = 14),
                CategoryRuleEntity(keyword = "customer", categoryId = 14, priority = 16)
            )
            ruleDao.insertAll(rules)
        }

        val settingsDao = targetDb.settingsDao()
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsert(AppSettingsEntity())
        }

        val parsingRuleDao = targetDb.parsingRuleDao()
        val defaultApps = listOf(
            "Google Pay" to "com.google.android.apps.nbu.paisa.user",
            "PhonePe" to "com.phonepe.app",
            "Paytm" to "net.one97.paytm",
            "Navi" to "com.naviapp",
            "BHIM UPI" to "in.org.npci.upiapp",
            "Amazon Pay" to "in.amazon.mShop.android.shopping",
            "CRED" to "com.dreamplug.androidapp",
            "WhatsApp Pay" to "com.whatsapp",
            "MobiKwik" to "com.mobikwik_new",
            "Freecharge" to "com.freecharge.android",
            "Super.money" to "money.super.payments",
            "Slice" to "indwin.c3.shareapp",
            "Jupiter" to "money.jupiter",
            "Fi Money" to "com.epifi.paisa",
            "Tata Neu" to "com.tatadigital.tcp",
            "BharatPe" to "com.bharatpe.app",
            "Airtel Payments" to "com.myairtelapp",
            "JioPay" to "com.jio.myjio",
            "HDFC Bank" to "com.snapwork.hdfc",
            "SBI YONO" to "com.sbi.lotusintouch",
            "ICICI iMobile" to "com.csam.icici.bank.imobile",
            "Axis Mobile" to "com.axis.mobile",
            "Kotak Bank" to "com.msf.kbank.mobile",
            "IDFC FIRST Bank" to "com.idfcfirstbank.optimus",
            "Federal Bank" to "com.fedmobile",
            "Bank of Baroda" to "com.bankofbaroda.mconnect",
            "Messages / Bank SMS" to "com.google.android.apps.messaging"
        )
        val existingPkgs = parsingRuleDao.getAllParsingRulesOnce()
            .map { it.packageName.lowercase() }
            .toSet()

        val missingRules = defaultApps
            .filter { (_, pkg) -> pkg.lowercase() !in existingPkgs }
            .map { (displayName, pkg) ->
                ParsingRuleEntity(
                    appDisplayName = displayName,
                    packageName = pkg,
                    amountRegex = DEFAULT_AMOUNT_REGEX,
                    sentKeywords = SENT_KEYWORDS,
                    receivedKeywords = RECEIVED_KEYWORDS,
                    referenceRegex = DEFAULT_REF_REGEX,
                    upiIdRegex = DEFAULT_UPI_REGEX,
                    phoneRegex = DEFAULT_PHONE_REGEX,
                    senderNameRegex = DEFAULT_SENDER_REGEX,
                    receiverNameRegex = DEFAULT_RECEIVER_REGEX
                )
            }
        if (missingRules.isNotEmpty()) {
            parsingRuleDao.insertAll(missingRules)
        }
    }
}
