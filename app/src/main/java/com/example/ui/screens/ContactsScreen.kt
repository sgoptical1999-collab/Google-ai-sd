package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CategoryEntity
import com.example.data.local.ContactEntity
import com.example.data.local.ContactLedgerSummary
import com.example.data.local.TransactionEntity
import com.example.domain.CurrencyFormatter
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.viewmodel.ContactsViewModel

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onOpenContactDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddContact by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContact = true },
                containerColor = EmeraldPrimary,
                contentColor = NavyDeep,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add contact")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Contacts & Party Ledger",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search contact name, UPI ID, or phone...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearch("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("contacts_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (state.contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No contacts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Contacts are identified automatically from UPI IDs, phone numbers, or names as transactions arrive.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactSummaryCard(
                            summary = contact,
                            currencySymbol = state.settings.currencySymbol,
                            onClick = { onOpenContactDetail(contact.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddContact) {
        ContactEditorDialog(
            initial = null,
            categories = state.categories,
            onDismiss = { showAddContact = false },
            onSave = { name, phone, upi, notes, defaultCat ->
                viewModel.createContact(name, phone, upi, notes, defaultCat)
                showAddContact = false
            }
        )
    }
}

@Composable
private fun ContactSummaryCard(
    summary: ContactLedgerSummary,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val net = summary.netSentMinusReceived
    val netText = when {
        net > 0.01 -> "${CurrencyFormatter.format(net, currencySymbol)} sent more than received"
        net < -0.01 -> "${CurrencyFormatter.format(kotlin.math.abs(net), currencySymbol)} received more than sent"
        else -> "Settled (₹0 net)"
    }
    val netColor = when {
        net > 0.01 -> ExpenseCoral
        net < -0.01 -> EmeraldPrimary
        else -> MutedSlate
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_card_${summary.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "UPI: ${summary.upiId} • Phone: ${summary.phoneNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${summary.transactionCount} txns",
                    style = MaterialTheme.typography.labelMedium,
                    color = InfoCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sent: ${CurrencyFormatter.format(summary.totalSent, currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExpenseCoral
                )
                Text(
                    text = "Received: ${CurrencyFormatter.format(summary.totalReceived, currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Net Balance: $netText",
                style = MaterialTheme.typography.labelLarge,
                color = netColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val detailFlow = remember(contactId) { viewModel.observeContactDetail(contactId) }
    val state by detailFlow.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    val contact = state.contact
    val symbol = state.settings.currencySymbol

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(contact?.name ?: "Contact Ledger") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("contact_detail_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (contact != null) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_contact_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit contact")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (contact != null) {
                item {
                    val net = state.netBalance
                    val netLabel = when {
                        net > 0.01 -> "${CurrencyFormatter.format(net, symbol)} sent more than received"
                        net < -0.01 -> "${CurrencyFormatter.format(kotlin.math.abs(net), symbol)} received more than sent"
                        else -> "Settled"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "UPI ID: ${contact.upiId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Phone,
                                    contentDescription = null,
                                    tint = MutedSlate,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Phone: ${contact.phoneNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSlate
                                )
                            }
                            if (contact.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = contact.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Money Sent", style = MaterialTheme.typography.labelMedium, color = MutedSlate)
                                    Text(
                                        text = CurrencyFormatter.format(state.totalSent, symbol),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseCoral
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Money Received", style = MaterialTheme.typography.labelMedium, color = MutedSlate)
                                    Text(
                                        text = CurrencyFormatter.format(state.totalReceived, symbol),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Net Balance: $netLabel",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (net > 0.01) ExpenseCoral else EmeraldPrimary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Payment History (${state.transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(state.transactions, key = { it.id }) { txn ->
                TransactionItemCard(
                    transaction = txn,
                    currencySymbol = symbol,
                    onClick = { onSelectTransaction(txn) }
                )
            }
        }
    }

    if (showEditDialog && contact != null) {
        ContactEditorDialog(
            initial = contact,
            categories = state.categories,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, upi, notes, defaultCat ->
                viewModel.updateContact(
                    contact.copy(
                        name = name,
                        phoneNumber = phone.ifBlank { TransactionEntity.UNKNOWN },
                        upiId = upi.ifBlank { TransactionEntity.UNKNOWN },
                        notes = notes,
                        defaultCategoryId = defaultCat
                    )
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ContactEditorDialog(
    initial: ContactEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, upiId: String, notes: String, defaultCategoryId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember {
        mutableStateOf(initial?.phoneNumber?.takeIf { it != TransactionEntity.UNKNOWN } ?: "")
    }
    var upi by remember {
        mutableStateOf(initial?.upiId?.takeIf { it != TransactionEntity.UNKNOWN } ?: "")
    }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var defaultCatId by remember { mutableStateOf(initial?.defaultCategoryId) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = { Text(if (initial == null) "New Contact" else "Edit Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Name *") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (if available)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upi,
                    onValueChange = { upi = it },
                    label = { Text("UPI ID / VPA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Auto-Category for this Contact", style = MaterialTheme.typography.labelLarge, color = MutedSlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = defaultCatId == null,
                        onClick = { defaultCatId = null },
                        label = { Text("Auto") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = defaultCatId == cat.id,
                            onClick = { defaultCatId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onSave(name.trim(), phone.trim(), upi.trim(), notes.trim(), defaultCatId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
