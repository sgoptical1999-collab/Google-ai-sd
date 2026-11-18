package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CurrencyFormatter
import com.example.domain.PersonReportItem
import com.example.domain.ReportPeriod
import com.example.ui.components.CategoryBreakdownChartCard
import com.example.ui.components.IncomeExpenseSplit100Card
import com.example.ui.components.IncomeVsExpenseChartCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsvToUri(context, it) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportPdfToUri(context, it) }
    }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val report = state.report
    val symbol = state.settings.currencySymbol

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Reports & Analytics",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            val periods = ReportPeriod.entries
            SecondaryTabRow(
                selectedTabIndex = periods.indexOf(state.selectedPeriod),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                periods.forEach { period ->
                    Tab(
                        selected = state.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        text = { Text(period.label) },
                        modifier = Modifier.testTag("report_tab_${period.name.lowercase()}")
                    )
                }
            }

            if (report != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Month switcher for end-of-month checks
                    if (state.selectedPeriod == ReportPeriod.MONTHLY) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = NavySurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                            contentDescription = "Previous month"
                                        )
                                    }
                                    Text(
                                        text = "${report.periodTitle} Statement",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { viewModel.shiftMonth(1) },
                                        enabled = state.monthOffset < 0
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = "Next month"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 100% Income vs Expense Percentage Split Card
                    item {
                        IncomeExpenseSplit100Card(
                            title = "${report.periodTitle} • Income vs Expense",
                            income = report.totalIncome,
                            expense = report.totalExpense,
                            incomePercent = report.incomePercent,
                            expensePercent = report.expensePercent,
                            currencySymbol = symbol
                        )
                    }

                    // Export Actions Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                            border = BorderStroke(1.dp, NavyBorder.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.FileDownload,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Export ${report.periodTitle} Report",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            csvLauncher.launch("SmartMoney_${report.periodTitle.replace(" ", "_")}.csv")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = NavyDeep
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_csv_button")
                                    ) {
                                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Excel CSV")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pdfLauncher.launch("SmartMoney_${report.periodTitle.replace(" ", "_")}.pdf")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_pdf_button")
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save PDF")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val file = viewModel.createShareableReportFile(context, isPdf = true)
                                                if (file != null) {
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("share_report_button")
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = "Share PDF report", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Period Totals
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportKpiTile(
                                label = "Income",
                                value = CurrencyFormatter.formatCompact(report.totalIncome, symbol),
                                color = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            ReportKpiTile(
                                label = "Expenses",
                                value = CurrencyFormatter.formatCompact(report.totalExpense, symbol),
                                color = ExpenseCoral,
                                modifier = Modifier.weight(1f)
                            )
                            ReportKpiTile(
                                label = "Net",
                                value = CurrencyFormatter.formatCompact(report.netCashflow, symbol),
                                color = if (report.netCashflow >= 0) EmeraldPrimary else ExpenseCoral,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Complete Unified List: Who Sent Me Money (Received From)
                    item {
                        CompletePartySummaryCard(
                            title = "Received From (Who Paid You)",
                            totalLabel = "Total Received: ${CurrencyFormatter.format(report.totalIncome, symbol)}",
                            items = report.allPeopleReceivedFrom,
                            isExpense = false,
                            currencySymbol = symbol
                        )
                    }

                    // Complete Unified List: Whom I Sent Money To (Paid To)
                    item {
                        CompletePartySummaryCard(
                            title = "Sent To (Whom You Paid)",
                            totalLabel = "Total Sent: ${CurrencyFormatter.format(report.totalExpense, symbol)}",
                            items = report.allPeoplePaid,
                            isExpense = true,
                            currencySymbol = symbol
                        )
                    }

                    // Payment App Breakdown (Paytm, PhonePe, Navi, GPay, etc.)
                    if (report.appBreakdown.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = NavySurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Apps, contentDescription = null, tint = InfoCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tracked Apps Summary",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    report.appBreakdown.forEach { app ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${app.appName} (${app.count} txns)",
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                if (app.totalReceived > 0) {
                                                    Text(
                                                        text = "+${CurrencyFormatter.format(app.totalReceived, symbol)}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = EmeraldPrimary
                                                    )
                                                }
                                                if (app.totalSent > 0) {
                                                    Text(
                                                        text = "-${CurrencyFormatter.format(app.totalSent, symbol)}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = ExpenseCoral
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Income vs Expense Chart
                    item {
                        IncomeVsExpenseChartCard(
                            title = "${report.period.label} Income vs Expense",
                            buckets = report.chartBuckets,
                            currencySymbol = symbol
                        )
                    }

                    // Category-wise Donut + List
                    item {
                        CategoryBreakdownChartCard(
                            breakdown = report.categoryBreakdown,
                            totalExpense = report.totalExpense,
                            currencySymbol = symbol
                        )
                    }

                    if (report.categoryBreakdown.isNotEmpty()) {
                        item {
                            Text(
                                text = "Category-wise Expense Report",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        itemsIndexed(report.categoryBreakdown) { _, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = NavySurface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.categoryName} (${item.count})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(item.totalAmount, symbol),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseCoral
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = EmeraldPrimary,
                                        trackColor = NavyBorder
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKpiTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedSlate)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun CompletePartySummaryCard(
    title: String,
    totalLabel: String,
    items: List<PersonReportItem>,
    isExpense: Boolean,
    currencySymbol: String
) {
    val accent = if (isExpense) ExpenseCoral else EmeraldPrimary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
            HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))

            if (items.isEmpty()) {
                Text(
                    text = "No entries recorded for this period yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            } else {
                items.forEachIndexed { idx, person ->
                    val amount = if (isExpense) person.totalSent else person.totalReceived
                    val count = if (isExpense) person.sentCount else person.receivedCount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${idx + 1}. ${person.personName}",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count payment${if (count != 1) "s" else ""} • UPI: ${person.upiId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = CurrencyFormatter.format(amount, currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}
