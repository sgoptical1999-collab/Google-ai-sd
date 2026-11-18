package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val indianSymbols = DecimalFormatSymbols(Locale("en", "IN"))
    private val indianFormat = DecimalFormat("##,##,##0.00", indianSymbols)
    private val indianWholeFormat = DecimalFormat("##,##,##0", indianSymbols)

    fun format(amount: Double, symbol: String = "₹", showDecimals: Boolean = true): String {
        val absVal = kotlin.math.abs(amount)
        val formatted = if (showDecimals) indianFormat.format(absVal) else indianWholeFormat.format(absVal)
        return if (amount < 0) "-$symbol$formatted" else "$symbol$formatted"
    }

    fun formatCompact(amount: Double, symbol: String = "₹"): String {
        val absVal = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 10_000_000 -> String.format(Locale.US, "%s%s%.2fCr", sign, symbol, absVal / 10_000_000.0)
            absVal >= 100_000 -> String.format(Locale.US, "%s%s%.2fL", sign, symbol, absVal / 100_000.0)
            absVal >= 1_000 -> String.format(Locale.US, "%s%s%.1fK", sign, symbol, absVal / 1_000.0)
            else -> format(amount, symbol, showDecimals = false)
        }
    }
}

object DateTimeUtils {
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val timeOnlyFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthLabelFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    fun formatDateTime(millis: Long): String = dateTimeFormatter.format(Date(millis))
    fun formatShortDate(millis: Long): String = shortDateFormatter.format(Date(millis))
    fun formatTime(millis: Long): String = timeOnlyFormatter.format(Date(millis))
    fun formatDayKey(millis: Long): String = dayKeyFormatter.format(Date(millis))
    fun formatMonthLabel(millis: Long): String = monthLabelFormatter.format(Date(millis))

    fun startOfTodayMillis(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfWeekMillis(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfMonthMillis(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfYearMillis(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
