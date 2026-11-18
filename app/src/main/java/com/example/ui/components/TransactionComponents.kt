package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.VerificationStatus
import com.example.domain.CurrencyFormatter
import com.example.domain.DateTimeUtils
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingContainer

fun categoryIconForName(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("food") || lower.contains("restaurant") -> Icons.Rounded.Restaurant
        lower.contains("shop") && !lower.contains("rent") -> Icons.Rounded.ShoppingBag
        lower.contains("grocery") -> Icons.Rounded.LocalGroceryStore
        lower.contains("electric") -> Icons.Rounded.Bolt
        lower.contains("mobile") || lower.contains("recharge") -> Icons.Rounded.PhoneAndroid
        lower.contains("fuel") || lower.contains("transport") -> Icons.Rounded.DirectionsCar
        lower.contains("salary") -> Icons.Rounded.Badge
        lower.contains("rent") -> Icons.Rounded.Storefront
        lower.contains("medical") -> Icons.Rounded.MedicalServices
        lower.contains("loan") -> Icons.Rounded.AccountBalance
        lower.contains("supplier") -> Icons.Rounded.Warehouse
        lower.contains("customer") -> Icons.Rounded.Payments
        lower.contains("business") -> Icons.Rounded.BusinessCenter
        lower.contains("personal") -> Icons.Rounded.Person
        lower.contains("income") -> Icons.AutoMirrored.Rounded.TrendingUp
        lower.contains("other") -> Icons.AutoMirrored.Rounded.ReceiptLong
        else -> Icons.Rounded.Category
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) EmeraldPrimary else ExpenseCoral
    val badgeBg = if (isIncome) EmeraldContainer else ExpenseContainer
    val isPending = transaction.verificationStatus == VerificationStatus.PENDING

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_card_${transaction.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = if (isPending) BorderStroke(1.dp, PendingAmber.copy(alpha = 0.55f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIconForName(transaction.categoryName),
                    contentDescription = transaction.categoryName,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.personName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPending) {
                        Surface(
                            color = PendingContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "REVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = PendingAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.categoryName} • ${transaction.paymentApp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val secondaryMeta = buildList {
                    add(DateTimeUtils.formatDateTime(transaction.timestamp))
                    if (transaction.upiId != TransactionEntity.UNKNOWN) add(transaction.upiId)
                    if (transaction.transactionReference != TransactionEntity.UNKNOWN) {
                        add("Ref: ${transaction.transactionReference}")
                    }
                }.joinToString(" • ")

                Text(
                    text = secondaryMeta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+" else "-") +
                        CurrencyFormatter.format(transaction.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = if (isIncome) "Received" else "Sent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
            }
        }
    }
}

@Composable
fun PendingReviewCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_card_${transaction.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
        border = BorderStroke(1.dp, PendingAmber.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Pending verification",
                        tint = PendingAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Needs Verification • ${transaction.paymentApp}",
                        style = MaterialTheme.typography.labelLarge,
                        color = PendingAmber
                    )
                }
                Text(
                    text = (if (isIncome) "+" else "-") +
                        CurrencyFormatter.format(transaction.amount, currencySymbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) EmeraldPrimary else ExpenseCoral
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Party: ${transaction.personName} • Category: ${transaction.categoryName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (transaction.upiId != TransactionEntity.UNKNOWN || transaction.phoneNumber != TransactionEntity.UNKNOWN) {
                Text(
                    text = "UPI: ${transaction.upiId} • Phone: ${transaction.phoneNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }
            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PendingAmber.copy(alpha = 0.9f)
                )
            }
            if (transaction.rawNotificationText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.rawNotificationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_pending_${transaction.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm")
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_pending_${transaction.id}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_pending_${transaction.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCoral),
                    border = BorderStroke(1.dp, ExpenseCoral.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete transaction", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionEditorDialog(
    initial: TransactionEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity, Boolean) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    var selectedType by remember { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var personName by remember {
        mutableStateOf(
            initial?.personName?.takeIf { it != TransactionEntity.UNKNOWN } ?: ""
        )
    }
    var phoneNumber by remember {
        mutableStateOf(
            initial?.phoneNumber?.takeIf { it != TransactionEntity.UNKNOWN } ?: ""
        )
    }
    var upiId by remember {
        mutableStateOf(
            initial?.upiId?.takeIf { it != TransactionEntity.UNKNOWN } ?: ""
        )
    }
    var paymentApp by remember {
        mutableStateOf(
            initial?.paymentApp?.takeIf { it != TransactionEntity.UNKNOWN } ?: "Manual Entry"
        )
    }
    var reference by remember {
        mutableStateOf(
            initial?.transactionReference?.takeIf { it != TransactionEntity.UNKNOWN } ?: ""
        )
    }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var selectedCategoryId by remember {
        mutableLongStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: 16L)
    }
    var rememberForContact by remember { mutableStateOf(true) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = {
            Text(
                text = if (initial == null) "Add Transaction" else "Edit & Verify Transaction",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = { Text("Expense / Sent") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseContainer,
                            selectedLabelColor = ExpenseCoral
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("Income / Rec.") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount *") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_amount_input")
                )

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person / Merchant Name") },
                    placeholder = { Text("Leave blank for Unknown") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_person_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone (if known)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID / VPA") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedSlate
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = paymentApp,
                        onValueChange = { paymentApp = it },
                        label = { Text("Payment App") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("UTR / Ref No.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberForContact = !rememberForContact }
                ) {
                    Checkbox(
                        checked = rememberForContact,
                        onCheckedChange = { rememberForContact = it }
                    )
                    Text(
                        text = "Remember this category for this contact",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmt = amountText.trim().toDoubleOrNull()
                    if (parsedAmt == null || parsedAmt <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    val chosenCat = categories.firstOrNull { it.id == selectedCategoryId }
                    val updated = (initial ?: TransactionEntity(
                        type = selectedType,
                        amount = parsedAmt,
                        timestamp = System.currentTimeMillis(),
                        categoryId = selectedCategoryId,
                        categoryName = chosenCat?.name ?: "Other Expenses"
                    )).copy(
                        type = selectedType,
                        amount = parsedAmt,
                        personName = personName.trim().ifBlank { TransactionEntity.UNKNOWN },
                        phoneNumber = phoneNumber.trim().ifBlank { TransactionEntity.UNKNOWN },
                        upiId = upiId.trim().ifBlank { TransactionEntity.UNKNOWN },
                        categoryId = selectedCategoryId,
                        categoryName = chosenCat?.name ?: "Other Expenses",
                        paymentApp = paymentApp.trim().ifBlank { "Manual Entry" },
                        transactionReference = reference.trim().ifBlank { TransactionEntity.UNKNOWN },
                        notes = notes.trim(),
                        verificationStatus = if (initial != null) VerificationStatus.EDITED else VerificationStatus.VERIFIED
                    )
                    onSave(updated, rememberForContact)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Save & Verify")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initial != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(initial) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseCoral)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
