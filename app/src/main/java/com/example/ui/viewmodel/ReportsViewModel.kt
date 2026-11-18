package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.FinancialReport
import com.example.domain.ReportGenerator
import com.example.domain.ReportPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val monthOffset: Int = 0,
    val report: FinancialReport? = null,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val exportMessage: String? = null
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val reportGenerator: ReportGenerator
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    private val monthOffset = MutableStateFlow(0)
    private val exportMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        transactionRepository.observeAll(),
        settingsRepository.observeSettings(),
        selectedPeriod,
        monthOffset,
        exportMessage
    ) { transactions, settings, period, offset, message ->
        ReportsUiState(
            selectedPeriod = period,
            monthOffset = offset,
            report = reportGenerator.generate(
                transactions = transactions,
                period = period,
                monthOffset = offset
            ),
            settings = settings,
            exportMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    fun selectPeriod(period: ReportPeriod) {
        selectedPeriod.value = period
    }

    fun shiftMonth(delta: Int) {
        val next = (monthOffset.value + delta).coerceAtMost(0)
        monthOffset.value = next
    }

    fun clearMessage() {
        exportMessage.value = null
    }

    fun exportCsvToUri(context: Context, uri: Uri) {
        val currentReport = uiState.value.report ?: return
        val symbol = uiState.value.settings.currencySymbol
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        reportGenerator.writeExcelCsv(currentReport, symbol, os)
                    } ?: error("Unable to open destination file")
                }
            }
            exportMessage.value = result.fold(
                onSuccess = { "Excel CSV report saved successfully." },
                onFailure = { "CSV export failed: ${it.message}" }
            )
        }
    }

    fun exportPdfToUri(context: Context, uri: Uri) {
        val currentReport = uiState.value.report ?: return
        val symbol = uiState.value.settings.currencySymbol
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        reportGenerator.writePdfReport(currentReport, symbol, os)
                    } ?: error("Unable to open destination file")
                }
            }
            exportMessage.value = result.fold(
                onSuccess = { "PDF financial report saved successfully." },
                onFailure = { "PDF export failed: ${it.message}" }
            )
        }
    }

    suspend fun createShareableReportFile(context: Context, isPdf: Boolean): File? {
        val currentReport = uiState.value.report ?: return null
        val symbol = uiState.value.settings.currencySymbol
        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "reports").apply { mkdirs() }
                val ext = if (isPdf) "pdf" else "csv"
                val file = File(dir, "smart_money_${currentReport.period.name.lowercase()}_report.$ext")
                file.outputStream().use { os ->
                    if (isPdf) {
                        reportGenerator.writePdfReport(currentReport, symbol, os)
                    } else {
                        reportGenerator.writeExcelCsv(currentReport, symbol, os)
                    }
                }
                file
            }.getOrNull()
        }
    }
}
