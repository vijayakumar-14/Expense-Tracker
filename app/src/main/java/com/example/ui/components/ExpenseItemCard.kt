package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.utils.CurrencyUtils

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onEditExpense: (Expense, String, Double) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    // Select suitable icons based on normalized type name
    val lowercaseName = expense.name.lowercase()
    val icon: ImageVector = when {
        lowercaseName.contains("milk") || lowercaseName.contains("coffee") || lowercaseName.contains("tea") || lowercaseName.contains("drink") -> Icons.Default.LocalCafe
        lowercaseName.contains("grocer") || lowercaseName.contains("market") || lowercaseName.contains("shop") || lowercaseName.contains("mart") || lowercaseName.contains("food") -> Icons.Default.ShoppingCart
        lowercaseName.contains("fuel") || lowercaseName.contains("gas") || lowercaseName.contains("shell") || lowercaseName.contains("petrol") -> Icons.Default.LocalGasStation
        lowercaseName.contains("car") || lowercaseName.contains("taxi") || lowercaseName.contains("ride") || lowercaseName.contains("uber") || lowercaseName.contains("transport") -> Icons.Default.DirectionsCar
        lowercaseName.contains("rent") || lowercaseName.contains("house") || lowercaseName.contains("room") -> Icons.Default.Home
        lowercaseName.contains("bill") || lowercaseName.contains("electricity") || lowercaseName.contains("power") || lowercaseName.contains("water") || lowercaseName.contains("internet") || lowercaseName.contains("phone") -> Icons.Default.ReceiptLong
        lowercaseName.contains("movie") || lowercaseName.contains("game") || lowercaseName.contains("show") || lowercaseName.contains("play") || lowercaseName.contains("fun") -> Icons.Default.ConfirmationNumber
        lowercaseName.contains("gym") || lowercaseName.contains("fit") || lowercaseName.contains("sport") || lowercaseName.contains("health") || lowercaseName.contains("pill") || lowercaseName.contains("med") -> Icons.Default.Favorite
        else -> Icons.Default.Paid
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("expense_item_${expense.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = expense.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = expense.date,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = CurrencyUtils.format(expense.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Edit Button
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("edit_button_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Expense",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = { onDeleteExpense(expense) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_button_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Expense",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showEditDialog) {
        EditExpenseDialog(
            expense = expense,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedName, updatedAmount ->
                onEditExpense(expense, updatedName, updatedAmount)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var editName by remember { mutableStateOf(expense.name) }
    var editAmountStr by remember { mutableStateOf(expense.amount.toString()) }
    var amountError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Expense",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it
                        nameError = it.trim().isEmpty()
                    },
                    label = { Text("Expense Category/Type") },
                    placeholder = { Text("e.g. Milk, Groceries") },
                    singleLine = true,
                    isError = nameError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editAmountStr,
                    onValueChange = {
                        editAmountStr = it
                        val parsed = it.toDoubleOrNull()
                        amountError = parsed == null || parsed <= 0.0
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = editAmountStr.toDoubleOrNull()
                    val isValidName = editName.trim().isNotEmpty()
                    val isValidAmount = parsedAmount != null && parsedAmount > 0.0

                    if (isValidName && isValidAmount) {
                        onConfirm(editName, parsedAmount!!)
                    } else {
                        nameError = !isValidName
                        amountError = !isValidAmount
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("edit_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
