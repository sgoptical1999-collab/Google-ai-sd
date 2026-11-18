package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.VerificationStatus
import com.example.ui.components.PendingReviewCard
import com.example.ui.components.TransactionEditorDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PendingAmber
import com.example.ui.viewmodel.DateRangeFilter
import com.example.ui.viewmodel.TransactionsViewModel
import com.example.ui.viewmodel.VerificationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    initialPendingOnly: Boolean = false,
    externalEditTransaction: TransactionEntity? = null,
    onConsumeExternalEdit: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTxn by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialPendingOnly) {
        if (initialPendingOnly) {
            viewModel.selectTab(VerificationTab.PENDING)
        }
    }

    LaunchedEffect(externalEditTransaction) {
        if (externalEditTransaction != null) {
            editingTxn = externalEditTransaction
            onConsumeExternalEdit()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = NavyDeep,
                modifier = Modifier.testTag("ledger_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Title + Filter button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions & Review",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.testTag("open_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter transactions",
                        tint = EmeraldPrimary
                    )
                }
            }

            // Verification Tabs: All / Pending Review / Verified
            val tabs = VerificationTab.entries
            SecondaryTabRow(
                selectedTabIndex = tabs.indexOf(state.filter.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEach { tab ->
                    val selected = state.filter.selectedTab == tab
                    Tab(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(tab.label)
                                if (tab == VerificationTab.PENDING && state.pendingCount > 0) {
                                    Badge(containerColor = PendingAmber, contentColor = NavyDeep) {
                                        Text("${state.pendingCount}")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Search Field
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = viewModel::updateQuery,
                placeholder = { Text("Search name, phone, UPI ID, UTR, category...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("transaction_search_input")
            )

            // Quick Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filter.typeFilter == TransactionType.INCOME,
                    onClick = {
                        viewModel.updateFilter {
                            it.copy(typeFilter = if (it.typeFilter == TransactionType.INCOME) null else TransactionType.INCOME)
                        }
                    },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = state.filter.typeFilter == TransactionType.EXPENSE,
                    onClick = {
                        viewModel.updateFilter {
                            it.copy(typeFilter = if (it.typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                        }
                    },
                    label = { Text("Expense") }
                )
                DateRangeFilter.entries.take(4).forEach { range ->
                    FilterChip(
                        selected = state.filter.dateRange == range,
                        onClick = { viewModel.updateFilter { it.copy(dateRange = range) } },
                        label = { Text(range.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (state.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.filter.selectedTab == VerificationTab.PENDING) {
                                "No pending transactions to verify"
                            } else {
                                "No matching transactions found"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Adjust your filters or wait for new UPI/bank alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.transactions, key = { it.id }) { txn ->
                        if (txn.verificationStatus == VerificationStatus.PENDING &&
                            state.filter.selectedTab == VerificationTab.PENDING
                        ) {
                            PendingReviewCard(
                                transaction = txn,
                                currencySymbol = state.settings.currencySymbol,
                                onConfirm = { viewModel.confirmTransaction(txn) },
                                onEdit = { editingTxn = txn },
                                onDelete = { viewModel.deleteTransaction(txn) }
                            )
                        } else {
                            TransactionItemCard(
                                transaction = txn,
                                currencySymbol = state.settings.currencySymbol,
                                onClick = { editingTxn = txn }
                            )
                        }
                    }
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
                viewModel.saveOrUpdateTransaction(txn)
                showAddDialog = false
            }
        )
    }

    editingTxn?.let { current ->
        TransactionEditorDialog(
            initial = current,
            categories = state.categories,
            onDismiss = { editingTxn = null },
            onSave = { updated, rememberForContact ->
                viewModel.confirmTransaction(updated, rememberForContact)
                editingTxn = null
            },
            onDelete = { toDelete ->
                viewModel.deleteTransaction(toDelete)
                editingTxn = null
            }
        )
    }

    if (showFilterDialog) {
        var minAmount by remember { mutableStateOf(state.filter.minAmount) }
        var maxAmount by remember { mutableStateOf(state.filter.maxAmount) }
        var selectedCatId by remember { mutableStateOf(state.filter.selectedCategoryId) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            containerColor = NavySurface,
            title = { Text("Filter Transactions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Amount Range", style = MaterialTheme.typography.labelLarge, color = MutedSlate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minAmount,
                            onValueChange = { minAmount = it },
                            label = { Text("Min ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxAmount,
                            onValueChange = { maxAmount = it },
                            label = { Text("Max ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Category", style = MaterialTheme.typography.labelLarge, color = MutedSlate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCatId == null,
                            onClick = { selectedCatId = null },
                            label = { Text("All Categories") }
                        )
                        state.categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFilter {
                            it.copy(
                                minAmount = minAmount,
                                maxAmount = maxAmount,
                                selectedCategoryId = selectedCatId
                            )
                        }
                        showFilterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetFilters()
                        showFilterDialog = false
                    }
                ) {
                    Text("Reset")
                }
            }
        )
    }
}
