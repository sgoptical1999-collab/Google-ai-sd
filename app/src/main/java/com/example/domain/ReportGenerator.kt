package com.example.domain

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class CategoryBreakdownItem(
    val categoryName: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class PersonReportItem(
    val personName: String,
    val upiId: String,
    val phoneNumber: String,
    val totalSent: Double,
    val totalReceived: Double,
    val netBalance: Double,
    val count: Int,
    val sentCount: Int = 0,
    val receivedCount: Int = 0
)

data class AppBreakdownItem(
    val appName: String,
    val totalSent: Double,
    val totalReceived: Double,
    val count: Int
)

data class PeriodChartBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

data class FinancialReport(
    val period: ReportPeriod,
    val periodTitle: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalVolume: Double,
    val incomePercent: Float,
    val expensePercent: Float,
    val netCashflow: Double,
    val transactionCount: Int,
    val chartBuckets: List<PeriodChartBucket>,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val personWiseReport: List<PersonReportItem>,
    val allPeoplePaid: List<PersonReportItem>,
    val allPeopleReceivedFrom: List<PersonReportItem>,
    val top10Paid: List<PersonReportItem>,
    val top10Received: List<PersonReportItem>,
    val appBreakdown: List<AppBreakdownItem>,
    val filteredTransactions: List<TransactionEntity>
)

class ReportGenerator {

    fun generate(
        transactions: List<TransactionEntity>,
        period: ReportPeriod,
        nowMillis: Long = System.currentTimeMillis(),
        monthOffset: Int = 0
    ): FinancialReport {
        val (startMillis, endMillis, periodTitle) = when (period) {
            ReportPeriod.DAILY -> Triple(
                DateTimeUtils.startOfTodayMillis(nowMillis),
                Long.MAX_VALUE,
                "Today"
            )
            ReportPeriod.WEEKLY -> Triple(
                DateTimeUtils.startOfWeekMillis(nowMillis),
                Long.MAX_VALUE,
                "This Week"
            )
            ReportPeriod.MONTHLY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    add(Calendar.MONTH, monthOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                val endCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val label = DateTimeUtils.formatMonthLabel(start)
                Triple(start, endCal.timeInMillis - 1L, label)
            }
            ReportPeriod.YEARLY -> Triple(
                DateTimeUtils.startOfYearMillis(nowMillis),
                Long.MAX_VALUE,
                Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.YEAR).toString()
            )
        }

        val scoped = transactions.filter { it.timestamp in startMillis..endMillis }
        val income = scoped.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = scoped.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalVolume = income + expense
        val incomePercent = if (totalVolume > 0.0) ((income / totalVolume) * 100.0).toFloat() else 0f
        val expensePercent = if (totalVolume > 0.0) ((expense / totalVolume) * 100.0).toFloat() else 0f

        // Category-wise expense breakdown
        val catBreakdown = scoped
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryName }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategoryBreakdownItem(
                    categoryName = cat,
                    totalAmount = sum,
                    count = list.size,
                    percentage = if (expense > 0) (sum / expense * 100.0).toFloat() else 0f
                )
            }
            .sortedByDescending { it.totalAmount }

        // Person-wise payment report (complete list for end-of-month reconciliation)
        val personItems = scoped
            .groupBy { txn ->
                when {
                    txn.contactId != null -> "contact:${txn.contactId}"
                    txn.upiId != TransactionEntity.UNKNOWN -> "upi:${txn.upiId.lowercase()}"
                    txn.personName != TransactionEntity.UNKNOWN -> "name:${txn.personName.lowercase()}"
                    else -> "unknown"
                }
            }
            .map { (_, list) ->
                val sample = list.firstOrNull { it.personName != TransactionEntity.UNKNOWN } ?: list.first()
                val sentTxns = list.filter { it.type == TransactionType.EXPENSE }
                val recTxns = list.filter { it.type == TransactionType.INCOME }
                val sent = sentTxns.sumOf { it.amount }
                val rec = recTxns.sumOf { it.amount }
                PersonReportItem(
                    personName = sample.personName,
                    upiId = list.firstOrNull { it.upiId != TransactionEntity.UNKNOWN }?.upiId ?: sample.upiId,
                    phoneNumber = list.firstOrNull { it.phoneNumber != TransactionEntity.UNKNOWN }?.phoneNumber ?: sample.phoneNumber,
                    totalSent = sent,
                    totalReceived = rec,
                    netBalance = rec - sent,
                    count = list.size,
                    sentCount = sentTxns.size,
                    receivedCount = recTxns.size
                )
            }

        val allPaid = personItems
            .filter { it.totalSent > 0 }
            .sortedByDescending { it.totalSent }

        val allReceived = personItems
            .filter { it.totalReceived > 0 }
            .sortedByDescending { it.totalReceived }

        val appBreakdown = scoped
            .groupBy { it.paymentApp.ifBlank { TransactionEntity.UNKNOWN } }
            .map { (appName, list) ->
                AppBreakdownItem(
                    appName = appName,
                    totalSent = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    totalReceived = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    count = list.size
                )
            }
            .sortedByDescending { it.totalSent + it.totalReceived }

        val buckets = buildChartBuckets(scoped, period, nowMillis, monthOffset)

        return FinancialReport(
            period = period,
            periodTitle = periodTitle,
            totalIncome = income,
            totalExpense = expense,
            totalVolume = totalVolume,
            incomePercent = incomePercent,
            expensePercent = expensePercent,
            netCashflow = income - expense,
            transactionCount = scoped.size,
            chartBuckets = buckets,
            categoryBreakdown = catBreakdown,
            personWiseReport = personItems.sortedByDescending { it.totalSent + it.totalReceived },
            allPeoplePaid = allPaid,
            allPeopleReceivedFrom = allReceived,
            top10Paid = allPaid.take(10),
            top10Received = allReceived.take(10),
            appBreakdown = appBreakdown,
            filteredTransactions = scoped
        )
    }

    private fun buildChartBuckets(
        transactions: List<TransactionEntity>,
        period: ReportPeriod,
        nowMillis: Long,
        monthOffset: Int
    ): List<PeriodChartBucket> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            if (period == ReportPeriod.MONTHLY) add(Calendar.MONTH, monthOffset)
        }
        return when (period) {
            ReportPeriod.DAILY -> {
                val labels = listOf("12a-6a", "6a-12p", "12p-6p", "6p-12a")
                val buckets = Array(4) { 0.0 to 0.0 }
                transactions.forEach { txn ->
                    val c = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
                    val idx = (c.get(Calendar.HOUR_OF_DAY) / 6).coerceIn(0, 3)
                    val cur = buckets[idx]
                    buckets[idx] = if (txn.type == TransactionType.INCOME) {
                        (cur.first + txn.amount) to cur.second
                    } else {
                        cur.first to (cur.second + txn.amount)
                    }
                }
                labels.mapIndexed { idx, label ->
                    PeriodChartBucket(label, buckets[idx].first, buckets[idx].second)
                }
            }
            ReportPeriod.WEEKLY -> {
                val fmt = SimpleDateFormat("EEE", Locale.getDefault())
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = DateTimeUtils.startOfWeekMillis(nowMillis)
                }
                (0 until 7).map { offset ->
                    val dayStart = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                    val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val dayTxns = transactions.filter {
                        it.timestamp >= dayStart.timeInMillis && it.timestamp < dayEnd.timeInMillis
                    }
                    PeriodChartBucket(
                        label = fmt.format(Date(dayStart.timeInMillis)),
                        income = dayTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expense = dayTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
            ReportPeriod.MONTHLY -> {
                (1..5).mapNotNull { weekIdx ->
                    val wTxns = transactions.filter { txn ->
                        val c = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
                        val wom = ((c.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
                        wom == weekIdx
                    }
                    if (weekIdx == 5 && wTxns.isEmpty() && cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= 28) {
                        null
                    } else {
                        PeriodChartBucket(
                            label = "W$weekIdx",
                            income = wTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                            expense = wTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        )
                    }
                }
            }
            ReportPeriod.YEARLY -> {
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val curMonth = cal.get(Calendar.MONTH)
                (0..curMonth).map { mIdx ->
                    val mTxns = transactions.filter { txn ->
                        val c = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
                        c.get(Calendar.MONTH) == mIdx
                    }
                    PeriodChartBucket(
                        label = monthNames[mIdx],
                        income = mTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expense = mTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
        }
    }

    fun writeExcelCsv(
        report: FinancialReport,
        currencySymbol: String,
        outputStream: OutputStream
    ) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Smart Money Tracker - ${report.period.label} (${report.periodTitle}) Financial Report")
        sb.appendLine("Generated,${csvCell(DateTimeUtils.formatDateTime(System.currentTimeMillis()))}")
        sb.appendLine("Total Income,${csvCell(CurrencyFormatter.format(report.totalIncome, currencySymbol))},${String.format(Locale.US, "%.1f%%", report.incomePercent)}")
        sb.appendLine("Total Expenses,${csvCell(CurrencyFormatter.format(report.totalExpense, currencySymbol))},${String.format(Locale.US, "%.1f%%", report.expensePercent)}")
        sb.appendLine("Net Cashflow,${csvCell(CurrencyFormatter.format(report.netCashflow, currencySymbol))}")
        sb.appendLine()

        sb.appendLine("Money Received From (Complete Party Summary)")
        sb.appendLine("Sender Name,UPI ID,Phone,Payments Count,Total Received")
        report.allPeopleReceivedFrom.forEach { p ->
            sb.appendLine(
                listOf(
                    p.personName,
                    p.upiId,
                    p.phoneNumber,
                    p.receivedCount.toString(),
                    String.format(Locale.US, "%.2f", p.totalReceived)
                ).joinToString(",") { csvCell(it) }
            )
        }
        sb.appendLine()

        sb.appendLine("Money Sent To (Complete Party Summary)")
        sb.appendLine("Receiver Name,UPI ID,Phone,Payments Count,Total Sent")
        report.allPeoplePaid.forEach { p ->
            sb.appendLine(
                listOf(
                    p.personName,
                    p.upiId,
                    p.phoneNumber,
                    p.sentCount.toString(),
                    String.format(Locale.US, "%.2f", p.totalSent)
                ).joinToString(",") { csvCell(it) }
            )
        }
        sb.appendLine()

        sb.appendLine("All Transactions")
        sb.appendLine("Date & Time,Type,Amount,Category,Person Name,Phone Number,UPI ID,Payment App,Reference / UTR,Status,Notes")
        report.filteredTransactions.forEach { t ->
            sb.appendLine(
                listOf(
                    DateTimeUtils.formatDateTime(t.timestamp),
                    t.type.name,
                    String.format(Locale.US, "%.2f", t.amount),
                    t.categoryName,
                    t.personName,
                    t.phoneNumber,
                    t.upiId,
                    t.paymentApp,
                    t.transactionReference,
                    t.verificationStatus.name,
                    t.notes
                ).joinToString(",") { csvCell(it) }
            )
        }
        sb.appendLine()
        sb.appendLine("Category-wise Expense Breakdown")
        sb.appendLine("Category,Transactions,Total Amount,Share %")
        report.categoryBreakdown.forEach { c ->
            sb.appendLine(
                listOf(
                    c.categoryName,
                    c.count.toString(),
                    String.format(Locale.US, "%.2f", c.totalAmount),
                    String.format(Locale.US, "%.1f%%", c.percentage)
                ).joinToString(",") { csvCell(it) }
            )
        }
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(sb.toString())
        }
    }

    private fun csvCell(raw: String): String {
        val escaped = raw.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun writePdfReport(
        report: FinancialReport,
        currencySymbol: String,
        outputStream: OutputStream
    ) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val headerPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
            isAntiAlias = true
        }
        val incomePaint = Paint(bodyPaint).apply {
            color = Color.parseColor("#059669")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val expensePaint = Paint(bodyPaint).apply {
            color = Color.parseColor("#E11D48")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 84f, headerPaint)
        canvas.drawText("Smart Money — ${report.periodTitle} (${report.period.label}) Report", margin, 38f, titlePaint)
        canvas.drawText(
            "Generated on ${DateTimeUtils.formatDateTime(System.currentTimeMillis())} • ${report.transactionCount} transactions",
            margin,
            58f,
            subtitlePaint
        )

        var y = 112f
        canvas.drawText("100% Income vs Expense Summary", margin, y, sectionPaint)
        y += 20f
        canvas.drawText(
            "Income: ${CurrencyFormatter.format(report.totalIncome, currencySymbol)} (${String.format(Locale.US, "%.1f%%", report.incomePercent)})",
            margin,
            y,
            incomePaint
        )
        canvas.drawText(
            "Expense: ${CurrencyFormatter.format(report.totalExpense, currencySymbol)} (${String.format(Locale.US, "%.1f%%", report.expensePercent)})",
            255f,
            y,
            expensePaint
        )
        canvas.drawText("Net: ${CurrencyFormatter.format(report.netCashflow, currencySymbol)}", 455f, y, bodyPaint)
        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 22f

        fun ensureSpace(needed: Float = 24f) {
            if (y > pageHeight - needed - 24f) {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        if (report.allPeopleReceivedFrom.isNotEmpty()) {
            canvas.drawText("Money Received From (Complete Summary)", margin, y, sectionPaint)
            y += 16f
            report.allPeopleReceivedFrom.forEach { p ->
                ensureSpace()
                canvas.drawText(p.personName.take(28), margin, y, bodyPaint)
                canvas.drawText(p.upiId.take(24), 220f, y, bodyPaint)
                canvas.drawText("${p.receivedCount} txns", 390f, y, bodyPaint)
                canvas.drawText(CurrencyFormatter.format(p.totalReceived, currencySymbol), 465f, y, incomePaint)
                y += 15f
            }
            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 22f
        }

        if (report.allPeoplePaid.isNotEmpty()) {
            ensureSpace(40f)
            canvas.drawText("Money Sent To (Complete Summary)", margin, y, sectionPaint)
            y += 16f
            report.allPeoplePaid.forEach { p ->
                ensureSpace()
                canvas.drawText(p.personName.take(28), margin, y, bodyPaint)
                canvas.drawText(p.upiId.take(24), 220f, y, bodyPaint)
                canvas.drawText("${p.sentCount} txns", 390f, y, bodyPaint)
                canvas.drawText(CurrencyFormatter.format(p.totalSent, currencySymbol), 465f, y, expensePaint)
                y += 15f
            }
            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 22f
        }

        ensureSpace(40f)
        canvas.drawText("Transactions", margin, y, sectionPaint)
        y += 16f
        canvas.drawText("Date", margin, y, bodyPaint)
        canvas.drawText("Counterparty", 130f, y, bodyPaint)
        canvas.drawText("Category", 285f, y, bodyPaint)
        canvas.drawText("App", 405f, y, bodyPaint)
        canvas.drawText("Amount", 495f, y, bodyPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 15f

        for (txn in report.filteredTransactions) {
            ensureSpace()
            val dateStr = DateTimeUtils.formatShortDate(txn.timestamp)
            val amtPaint = if (txn.type == TransactionType.INCOME) incomePaint else expensePaint
            val amtStr = (if (txn.type == TransactionType.INCOME) "+" else "-") +
                CurrencyFormatter.format(txn.amount, currencySymbol)

            canvas.drawText(dateStr, margin, y, bodyPaint)
            canvas.drawText(txn.personName.take(24), 130f, y, bodyPaint)
            canvas.drawText(txn.categoryName.take(18), 285f, y, bodyPaint)
            canvas.drawText(txn.paymentApp.take(14), 405f, y, bodyPaint)
            canvas.drawText(amtStr, 495f, y, amtPaint)
            y += 16f
        }

        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }
}
