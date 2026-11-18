package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DatabaseSeeder
import com.example.data.local.SmartMoneyDatabase
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.VerificationStatus
import com.example.domain.CategorizationEngine
import com.example.domain.ContactMatcher
import com.example.domain.IngestionResult
import com.example.domain.ParseOutcome
import com.example.domain.SmartMoneyNotificationParser
import com.example.domain.TransactionIngestionPipeline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartMoneyPipelineTest {

    private lateinit var db: SmartMoneyDatabase
    private lateinit var pipeline: TransactionIngestionPipeline
    private lateinit var parser: SmartMoneyNotificationParser

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SmartMoneyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseSeeder.seedDefaults(db)

        parser = SmartMoneyNotificationParser()
        val contactMatcher = ContactMatcher(db.contactDao())
        val categorizationEngine = CategorizationEngine(
            categoryDao = db.categoryDao(),
            categoryRuleDao = db.categoryRuleDao(),
            transactionDao = db.transactionDao()
        )
        pipeline = TransactionIngestionPipeline(
            parser = parser,
            contactMatcher = contactMatcher,
            categorizationEngine = categorizationEngine,
            transactionDao = db.transactionDao(),
            settingsDao = db.settingsDao(),
            parsingRuleDao = db.parsingRuleDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `otp and pin notifications are strictly ignored`() = runTest {
        val rules = db.parsingRuleDao().getEnabledParsingRulesOnce()
        val otpOutcome = parser.parse(
            packageName = "com.google.android.apps.messaging",
            title = "HDFC Bank",
            body = "Your OTP for Rs 5,000 transaction is 481920. Do not share with anyone.",
            timestamp = 1_700_000_000_000L,
            rules = rules
        )
        assertTrue(otpOutcome is ParseOutcome.Ignored)

        val pinOutcome = parser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            body = "Enter UPI PIN to authorize payment of Rs 1,200",
            timestamp = 1_700_000_000_000L,
            rules = rules
        )
        assertTrue(pinOutcome is ParseOutcome.Ignored)
    }

    @Test
    fun `upi id with 10 digits is never mistaken for mobile number`() = runTest {
        val rules = db.parsingRuleDao().getEnabledParsingRulesOnce()
        val outcome = parser.parse(
            packageName = "net.one97.paytm",
            title = "Paytm",
            body = "Paid ₹450.00 to 9876543210@paytm via UPI Ref No 432198765432",
            timestamp = 1_700_000_000_000L,
            rules = rules
        )
        assertTrue(outcome is ParseOutcome.Parsed)
        val parsed = (outcome as ParseOutcome.Parsed).data
        assertEquals("9876543210@paytm", parsed.upiId)
        assertEquals(TransactionEntity.UNKNOWN, parsed.phoneNumber)
        assertEquals("432198765432", parsed.transactionReference)
    }

    @Test
    fun `pipeline extracts fields matches contact categorizes and blocks duplicates`() = runTest {
        val t1 = pipeline.ingestNotification(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            body = "Paid ₹5,000 to Rahul rahul@upi Mob: 9811223344 Ref No 998877665544",
            timestamp = 1_700_000_000_000L
        )
        assertTrue(t1 is IngestionResult.Saved)
        val saved1 = (t1 as IngestionResult.Saved).transaction
        assertEquals(TransactionType.EXPENSE, saved1.type)
        assertEquals(5000.0, saved1.amount, 0.01)
        assertEquals("Rahul", saved1.personName)
        assertEquals("rahul@upi", saved1.upiId)
        assertEquals("9811223344", saved1.phoneNumber)
        assertEquals("998877665544", saved1.transactionReference)
        assertEquals(VerificationStatus.VERIFIED, saved1.verificationStatus)

        // Duplicate UTR must be blocked
        val dup = pipeline.ingestNotification(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            body = "Paid ₹5,000 to Rahul rahul@upi Ref No 998877665544",
            timestamp = 1_700_000_010_000L
        )
        assertTrue(dup is IngestionResult.Duplicate)

        // Incoming payment from same UPI ID links to existing Rahul contact
        val t2 = pipeline.ingestNotification(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            body = "Received ₹2,000 from rahul@upi UTR 112233445566",
            timestamp = 1_700_000_500_000L
        )
        assertTrue(t2 is IngestionResult.Saved)
        val saved2 = (t2 as IngestionResult.Saved).transaction
        assertEquals(saved1.contactId, saved2.contactId)

        val summaries = db.contactDao().observeContactSummaries().first()
        val rahul = summaries.first { it.id == saved1.contactId }
        assertEquals(5000.0, rahul.totalSent, 0.01)
        assertEquals(2000.0, rahul.totalReceived, 0.01)
        assertEquals(3000.0, rahul.netSentMinusReceived, 0.01)
    }

    @Test
    fun `missing name flags transaction as pending review instead of inventing name`() = runTest {
        val res = pipeline.ingestNotification(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            body = "INR 850.00 debited from A/c XX1234 via UPI",
            timestamp = 1_700_000_100_000L
        )
        assertTrue(res is IngestionResult.Saved)
        val txn = (res as IngestionResult.Saved).transaction
        assertEquals(TransactionEntity.UNKNOWN, txn.personName)
        assertEquals(TransactionEntity.UNKNOWN, txn.phoneNumber)
        assertEquals(TransactionEntity.UNKNOWN, txn.upiId)
        assertEquals(VerificationStatus.PENDING, txn.verificationStatus)
    }

    @Test
    fun `navi and any installed payment app are tracked automatically and monthly 100 percent split works`() = runTest {
        val now = System.currentTimeMillis()
        val naviRes = pipeline.ingestNotification(
            packageName = "com.naviapp",
            title = "Navi",
            body = "Paid ₹1,000 to Metro Mart metromart@okaxis UTR 554433221100",
            timestamp = now
        )
        assertTrue(naviRes is IngestionResult.Saved)
        assertEquals("Navi", (naviRes as IngestionResult.Saved).transaction.paymentApp)

        val customAppRes = pipeline.ingestNotification(
            packageName = "com.regional.coopbank",
            title = "Co-op Bank",
            body = "Received ₹3,000 from Amit Verma amit@upi Ref 778899001122",
            timestamp = now + 5_000L,
            resolvedAppLabel = "Regional Co-op Bank"
        )
        assertTrue(customAppRes is IngestionResult.Saved)
        assertEquals("Regional Co-op Bank", (customAppRes as IngestionResult.Saved).transaction.paymentApp)

        val allTxns = db.transactionDao().observeAllTransactions().first()
        val report = com.example.domain.ReportGenerator().generate(
            transactions = allTxns,
            period = com.example.domain.ReportPeriod.MONTHLY,
            nowMillis = now + 10_000L
        )
        assertEquals(3000.0, report.totalIncome, 0.01)
        assertEquals(1000.0, report.totalExpense, 0.01)
        assertEquals(75.0f, report.incomePercent, 0.1f)
        assertEquals(25.0f, report.expensePercent, 0.1f)
        assertEquals(1, report.allPeopleReceivedFrom.size)
        assertEquals("Amit Verma", report.allPeopleReceivedFrom.first().personName)
        assertEquals(1, report.allPeoplePaid.size)
        assertEquals("Metro Mart", report.allPeoplePaid.first().personName)
    }
}
