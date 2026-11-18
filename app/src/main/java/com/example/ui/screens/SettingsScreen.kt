package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.CategoryScope
import com.example.data.local.ParsingRuleEntity
import com.example.domain.CurrencyFormatter
import com.example.service.InstalledPaymentAppsHelper
import com.example.service.NotificationPermissionHelper
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
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
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccess by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationAccessGranted(context))
    }
    val installedUpiApps = remember(context) {
        InstalledPaymentAppsHelper.getInstalledUpiApps(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = NotificationPermissionHelper.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showBalanceDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingParsingRule by remember { mutableStateOf<ParsingRuleEntity?>(null) }
    var showNewParsingRule by remember { mutableStateOf(false) }

    // Live Parser Tester State
    var testPackage by remember { mutableStateOf("com.google.android.apps.nbu.paisa.user") }
    var testTitle by remember { mutableStateOf("Google Pay") }
    var testBody by remember {
        mutableStateOf("Paid ₹1,250.00 to Rahul Traders rahul@okicici via UPI Ref No 431829401234")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Settings & Privacy",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Security & Privacy Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_privacy_banner),
                        contentDescription = "Offline financial privacy vault illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        NavyDeep.copy(alpha = 0.94f),
                                        NavyPrimary.copy(alpha = 0.78f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Privacy-First Offline Storage",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All transactions stay encrypted-safe on your device SQLite database. We never capture OTPs, PINs, passwords, or initiate payments.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Notification Access Permission & Monitoring Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(
                        1.dp,
                        if (hasAccess) EmeraldPrimary.copy(alpha = 0.4f) else PendingAmber.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (hasAccess) EmeraldPrimary else PendingAmber
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notification Access",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Surface(
                                color = if (hasAccess) EmeraldContainer else PendingContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (hasAccess) "GRANTED" else "REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasAccess) EmeraldPrimary else PendingAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Smart Money reads incoming UPI & bank payment notifications locally on your phone to log amounts, UTR references, and party details. OTPs, MPINs, and auth codes are immediately discarded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )

                        Button(
                            onClick = { NotificationPermissionHelper.openNotificationAccessSettings(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasAccess) NavySurfaceElevated else EmeraldPrimary,
                                contentColor = if (hasAccess) MaterialTheme.colorScheme.onSurface else NavyDeep
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_notification_access_btn")
                        ) {
                            Text(if (hasAccess) "Manage Android Notification Access" else "Grant Notification Access")
                        }

                        SettingSwitchRow(
                            title = "Enable Transaction Monitoring",
                            subtitle = "Pause or resume automatic detection anytime",
                            checked = state.settings.monitoringEnabled,
                            onCheckedChange = viewModel::setMonitoringEnabled,
                            tag = "switch_monitoring"
                        )
                        SettingSwitchRow(
                            title = "Auto-Track All Installed Payment Apps",
                            subtitle = "Track Paytm, PhonePe, Navi, GPay & any banking app automatically",
                            checked = state.settings.trackAllInstalledPaymentApps,
                            onCheckedChange = viewModel::setTrackAllInstalledPaymentApps,
                            tag = "switch_track_all_apps"
                        )
                        SettingSwitchRow(
                            title = "Automatic Categorization",
                            subtitle = "Assign categories using contact history & rules",
                            checked = state.settings.autoCategorizeEnabled,
                            onCheckedChange = viewModel::setAutoCategorizeEnabled,
                            tag = "switch_autocat"
                        )
                        SettingSwitchRow(
                            title = "Flag Unknown Counterparties for Review",
                            subtitle = "Send incomplete alerts to Pending Transactions",
                            checked = state.settings.flagUnknownSenderAsPending,
                            onCheckedChange = viewModel::setFlagUnknownAsPending,
                            tag = "switch_pending_flag"
                        )
                    }
                }
            }

            // Opening Balance & Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBalanceDialog = true },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ledger & Opening Balance",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${state.settings.businessName} • Opening: ${
                                    CurrencyFormatter.format(state.settings.openingBalance, state.settings.currencySymbol)
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit opening balance", tint = EmeraldPrimary)
                    }
                }
            }

            // Interactive Notification Parser Verification Tool
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, InfoCyan.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BugReport, contentDescription = null, tint = InfoCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test Notification Parser",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Paste or tap a sample UPI/bank alert to test real extraction, deduplication, OTP filtering, and contact matching.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testPackage = "com.google.android.apps.nbu.paisa.user"
                                    testTitle = "Google Pay"
                                    testBody = "Paid ₹1,250.00 to Rahul Traders rahul@okicici via UPI Ref No 431829401234"
                                },
                                label = { Text("GPay Sent") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testPackage = "com.phonepe.app"
                                    testTitle = "PhonePe"
                                    testBody = "Received ₹5,000 from Priya Sharma (priya@ybl) Mob: 9876543210 UTR 519283746512"
                                },
                                label = { Text("PhonePe Rec.") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testPackage = "com.naviapp"
                                    testTitle = "Navi"
                                    testBody = "Paid ₹750.00 to Metro Mart metromart@okaxis UTR 665544332211"
                                },
                                label = { Text("Navi UPI") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testPackage = "com.google.android.apps.messaging"
                                    testTitle = "HDFC Bank"
                                    testBody = "Your OTP for Rs 2,500 transaction is 482910. Do not share with anyone."
                                },
                                label = { Text("OTP Filter Test") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testPackage = "net.one97.paytm"
                                    testTitle = "Paytm"
                                    testBody = "Debited Rs. 450.00 via UPI 9988776655@paytm"
                                },
                                label = { Text("Incomplete Alert") }
                            )
                        }

                        OutlinedTextField(
                            value = testTitle,
                            onValueChange = { testTitle = it },
                            label = { Text("Notification Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = testBody,
                            onValueChange = { testBody = it },
                            label = { Text("Notification Body") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parser_test_body_input")
                        )

                        Button(
                            onClick = {
                                viewModel.testNotificationParser(testPackage, testTitle, testBody)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = InfoCyan, contentColor = NavyDeep),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_parser_test_button")
                        ) {
                            Text("Run Live Parser & Save to Ledger")
                        }

                        state.parserTestStatus?.let { resultMsg ->
                            Surface(
                                color = NavySurfaceElevated,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = resultMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                        .testTag("parser_test_result")
                                )
                            }
                        }
                    }
                }
            }

            // Categories & Keyword Rules
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Category, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Categories (${state.categories.size})", style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(onClick = { showCategoryDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.categories.forEach { cat ->
                                FilterChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(cat.name) },
                                    trailingIcon = if (cat.isCustom) {
                                        {
                                            IconButton(
                                                onClick = { viewModel.deleteCategory(cat) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Delete custom category",
                                                    tint = ExpenseCoral,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    } else null
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.Rule, contentDescription = null, tint = InfoCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto-Category Rules (${state.categoryRules.size})", style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(onClick = { showRuleDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Rule")
                            }
                        }

                        state.categoryRules.take(8).forEach { rule ->
                            val catName = state.categories.firstOrNull { it.id == rule.categoryId }?.name ?: "Category"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"${rule.keyword}\" → $catName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { viewModel.deleteCategoryRule(rule) }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete rule",
                                        tint = MutedSlate,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detected Installed UPI Apps on Device
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Installed Payment Apps on This Phone (${installedUpiApps.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (installedUpiApps.isEmpty()) {
                            Text(
                                text = "All UPI & banking apps (Paytm, PhonePe, Navi, GPay, bank apps, SMS) are tracked automatically via Notification Access.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                installedUpiApps.forEach { app ->
                                    FilterChip(
                                        selected = true,
                                        onClick = {},
                                        label = { Text(app.appName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Configurable Notification Parsing Rules
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("App Parsing Rules (${state.parsingRules.size})", style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(onClick = { showNewParsingRule = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add App")
                            }
                        }

                        state.parsingRules.forEach { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingParsingRule = rule }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rule.appDisplayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = rule.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedSlate,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.saveParsingRule(rule.copy(isEnabled = enabled))
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Offline & Future Encrypted Cloud Backup Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MutedSlate)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Offline SQLite Mode Active",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Zero data leaves this device. End-to-end encrypted cloud backup is planned for a future release.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBalanceDialog) {
        var balanceText by remember { mutableStateOf(state.settings.openingBalance.toString()) }
        var bizName by remember { mutableStateOf(state.settings.businessName) }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            containerColor = NavySurface,
            title = { Text("Ledger Profile & Opening Balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bizName,
                        onValueChange = { bizName = it },
                        label = { Text("Account / Business Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Opening Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBusinessName(bizName)
                        balanceText.toDoubleOrNull()?.let { viewModel.updateOpeningBalance(it) }
                        showBalanceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var scopeChoice by remember { mutableStateOf(CategoryScope.EXPENSE) }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = NavySurface,
            title = { Text("Create Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryScope.entries.forEach { sc ->
                            FilterChip(
                                selected = scopeChoice == sc,
                                onClick = { scopeChoice = sc },
                                label = { Text(sc.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.createCategory(catName, scopeChoice, 0xFF10B981)
                            showCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRuleDialog) {
        var keyword by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableLongStateOf(state.categories.firstOrNull()?.id ?: 1L) }
        AlertDialog(
            onDismissRequest = { showRuleDialog = false },
            containerColor = NavySurface,
            title = { Text("Add Auto-Category Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Keyword, Name, or UPI ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                        if (keyword.isNotBlank()) {
                            viewModel.saveCategoryRule(keyword, selectedCatId)
                            showRuleDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Save Rule") }
            },
            dismissButton = {
                TextButton(onClick = { showRuleDialog = false }) { Text("Cancel") }
            }
        )
    }

    val activeRule = editingParsingRule ?: if (showNewParsingRule) {
        ParsingRuleEntity(
            appDisplayName = "",
            packageName = "",
            amountRegex = """(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
            sentKeywords = "paid,sent,debited,spent,transferred",
            receivedKeywords = "received,credited,deposited,refund",
            referenceRegex = """(?:UTR|Ref(?:\s*No)?|Txn\s*ID)\s*[:#\-]?\s*([A-Za-z0-9]{6,24})""",
            upiIdRegex = """([a-zA-Z0-9._\-]{2,}@[a-zA-Z0-9]{2,})""",
            phoneRegex = """(?:(?:Mob|Mobile|Phone|Ph|Contact)\s*[:#\-]?\s*)((?:\+91[\s\-]?)?[6-9][0-9]{9})(?!@)""",
            senderNameRegex = """(?:from|by)\s+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})""",
            receiverNameRegex = """(?:to|at)\s+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""
        )
    } else null

    if (activeRule != null) {
        var displayName by remember(activeRule) { mutableStateOf(activeRule.appDisplayName) }
        var pkgName by remember(activeRule) { mutableStateOf(activeRule.packageName) }
        var sentKw by remember(activeRule) { mutableStateOf(activeRule.sentKeywords) }
        var recKw by remember(activeRule) { mutableStateOf(activeRule.receivedKeywords) }
        var amtRegex by remember(activeRule) { mutableStateOf(activeRule.amountRegex) }

        AlertDialog(
            onDismissRequest = {
                editingParsingRule = null
                showNewParsingRule = false
            },
            containerColor = NavySurface,
            title = { Text(if (activeRule.id == 0L) "New App Parsing Rule" else "Edit ${activeRule.appDisplayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("App Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pkgName,
                        onValueChange = { pkgName = it },
                        label = { Text("Android Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sentKw,
                        onValueChange = { sentKw = it },
                        label = { Text("Sent Keywords (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recKw,
                        onValueChange = { recKw = it },
                        label = { Text("Received Keywords (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amtRegex,
                        onValueChange = { amtRegex = it },
                        label = { Text("Amount Regex") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (displayName.isNotBlank() && pkgName.isNotBlank()) {
                            viewModel.saveParsingRule(
                                activeRule.copy(
                                    appDisplayName = displayName.trim(),
                                    packageName = pkgName.trim(),
                                    sentKeywords = sentKw,
                                    receivedKeywords = recKw,
                                    amountRegex = amtRegex
                                )
                            )
                            editingParsingRule = null
                            showNewParsingRule = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (activeRule.id != 0L) {
                        TextButton(
                            onClick = {
                                viewModel.deleteParsingRule(activeRule)
                                editingParsingRule = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseCoral)
                        ) { Text("Delete") }
                    }
                    TextButton(
                        onClick = {
                            editingParsingRule = null
                            showNewParsingRule = false
                        }
                    ) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary)
        )
    }
}
