package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.domain.CurrencyFormatter
import com.example.service.NotificationPermissionHelper
import com.example.ui.components.CategoryBreakdownChartCard
import com.example.ui.components.IncomeExpenseSplit100Card
import com.example.ui.components.IncomeVsExpenseChartCard
import com.example.ui.components.TransactionEditorDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingContainer
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenTransactions: (pendingOnly: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationAccessGranted(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = NotificationPermissionHelper.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    val symbol = state.settings.currencySymbol

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = NavyDeep,
                modifier = Modifier.testTag("dashboard_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add manual transaction")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Live Listener Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Money Tracker",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${state.settings.businessName} • Offline Ledger",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                    }

                    ListenerStatusPill(
                        permissionGranted = hasNotificationPermission,
                        monitoringEnabled = state.settings.monitoringEnabled,
                        onClick = {
                            if (!hasNotificationPermission) {
                                NotificationPermissionHelper.openNotificationAccessSettings(context)
                            } else {
                                onOpenSettings()
                            }
                        }
                    )
                }
            }

            // Notification Access Onboarding Banner when not granted
            item {
                AnimatedVisibility(visible = !hasNotificationPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { NotificationPermissionHelper.openNotificationAccessSettings(context) },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                        border = BorderStroke(1.dp, InfoCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(InfoCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = InfoCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Auto-Tracking",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Grant Notification Access to detect UPI & bank alerts locally. OTPs & PINs are never captured.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSlate
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Open settings",
                                tint = InfoCyan
                            )
                        }
                    }
                }
            }

            // Hero Available Balance + Total Received / Sent Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F2942),
                                    NavySurface,
                                    Color(0xFF082D2B)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Available Balance",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MutedSlate
                                )
                            }
                            Surface(
                                color = EmeraldContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "100% On-Device",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = CurrencyFormatter.format(state.availableBalance, symbol),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("dashboard_available_balance")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroFlowMiniTile(
                                label = "Total Received",
                                value = CurrencyFormatter.format(state.totalMoneyReceived, symbol),
                                icon = Icons.Rounded.ArrowDownward,
                                accent = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            HeroFlowMiniTile(
                                label = "Total Sent",
                                value = CurrencyFormatter.format(state.totalMoneySent, symbol),
                                icon = Icons.Rounded.ArrowUpward,
                                accent = ExpenseCoral,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Pending Verification Banner
            item {
                AnimatedVisibility(visible = state.pendingCount > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_pending_banner")
                            .clickable { onOpenTransactions(true) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = PendingContainer),
                        border = BorderStroke(1.dp, PendingAmber.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PendingActions,
                                    contentDescription = null,
                                    tint = PendingAmber
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${state.pendingCount} Pending Transaction${if (state.pendingCount > 1) "s" else ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PendingAmber
                                    )
                                    Text(
                                        text = "Tap to review & confirm incomplete alert details",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Review pending transactions",
                                tint = PendingAmber
                            )
                        }
                    }
                }
            }

            // Today & Monthly Metrics 2x2 Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Today's Income",
                            value = CurrencyFormatter.format(state.todayIncome, symbol),
                            icon = Icons.Rounded.Today,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Today's Expenses",
                            value = CurrencyFormatter.format(state.todayExpense, symbol),
                            icon = Icons.Rounded.Today,
                            accentColor = ExpenseCoral,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Monthly Income",
                            value = CurrencyFormatter.format(state.monthlyIncome, symbol),
                            icon = Icons.Rounded.CalendarMonth,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Monthly Expenses",
                            value = CurrencyFormatter.format(state.monthlyExpense, symbol),
                            icon = Icons.Rounded.CalendarMonth,
                            accentColor = ExpenseCoral,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Monthly 100% Income vs Expense Split
            item {
                state.monthlyReport?.let { mReport ->
                    IncomeExpenseSplit100Card(
                        title = "${mReport.periodTitle} • Income vs Expense",
                        income = mReport.totalIncome,
                        expense = mReport.totalExpense,
                        incomePercent = mReport.incomePercent,
                        expensePercent = mReport.expensePercent,
                        currencySymbol = symbol
                    )
                }
            }

            // Weekly Cashflow Chart
            item {
                state.weeklyReport?.let { report ->
                    IncomeVsExpenseChartCard(
                        title = "This Week: Income vs Expenses",
                        buckets = report.chartBuckets,
                        currencySymbol = symbol
                    )
                }
            }

            // Category Donut Chart
            item {
                state.weeklyReport?.let { report ->
                    CategoryBreakdownChartCard(
                        breakdown = report.categoryBreakdown,
                        totalExpense = report.totalExpense,
                        currencySymbol = symbol
                    )
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { onOpenTransactions(false) }) {
                        Text("View All")
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Ready to Track Automatically",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incoming and outgoing UPI/bank notifications will appear here automatically, or tap + to log a cash/manual entry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    }
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { txn ->
                    TransactionItemCard(
                        transaction = txn,
                        currencySymbol = symbol,
                        onClick = { onEditTransaction(txn) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        TransactionEditorDialog(
            initial = null,
            categories = state.categories,
            onDismiss = { showAddDialog = false },
            onSave = { txn, _ ->
                viewModel.addManualTransaction(
                    type = txn.type,
                    amount = txn.amount,
                    personName = txn.personName,
                    phoneNumber = txn.phoneNumber,
                    upiId = txn.upiId,
                    categoryId = txn.categoryId,
                    categoryName = txn.categoryName,
                    paymentApp = txn.paymentApp,
                    reference = txn.transactionReference,
                    notes = txn.notes
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ListenerStatusPill(
    permissionGranted: Boolean,
    monitoringEnabled: Boolean,
    onClick: () -> Unit
) {
    val (bg, fg, label, icon) = when {
        !permissionGranted -> Quad(PendingContainer, PendingAmber, "Setup Needed", Icons.Rounded.NotificationsOff)
        !monitoringEnabled -> Quad(ExpenseContainer, ExpenseCoral, "Paused", Icons.Rounded.NotificationsOff)
        else -> Quad(EmeraldContainer, EmeraldPrimary, "Listening", Icons.Rounded.NotificationsActive)
    }
    Surface(
        modifier = Modifier
            .testTag("listener_status_pill")
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = fg)
        }
    }
}

@Composable
private fun HeroFlowMiniTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = NavyPrimary.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NavyBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedSlate)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
