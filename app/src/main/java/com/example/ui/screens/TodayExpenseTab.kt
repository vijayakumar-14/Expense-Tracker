package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.ExpenseViewModel
import com.example.ui.components.ExpenseItemCard
import com.example.utils.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayExpenseTab(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val todayExpenses by viewModel.todayExpenses.collectAsState()
    val autocompleteSuggestions by viewModel.autocompleteSuggestions.collectAsState()

    var expenseName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isNameFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Formatting date
    val todayDate = viewModel.today
    val formattedDate = remember(todayDate) {
        todayDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
    }

    // Filter autocomplete based on input: case-insensitive, top 5 closest matches
    val filteredSuggestions = remember(expenseName, autocompleteSuggestions) {
        if (expenseName.isBlank()) {
            autocompleteSuggestions.take(5)
        } else {
            autocompleteSuggestions
                .filter { it.contains(expenseName, ignoreCase = true) }
                .sortedWith(compareBy<String> {
                    if (it.equals(expenseName, ignoreCase = true)) 0 else 1
                }.thenBy {
                    if (it.startsWith(expenseName, ignoreCase = true)) 0 else 1
                }.thenBy {
                    it.length
                }.thenBy {
                    it.lowercase()
                })
                .take(5)
        }
    }

    val runningTotal = remember(todayExpenses) {
        todayExpenses.sumOf { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_expense_tab_column")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TODAY'S SPENDING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = formattedDate,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // High Density Spending Card (Mockup Theme Accent)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_total_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Total Spent Today",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.format(runningTotal),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Add Quick Expense Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_quick_expense_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ADD QUICK EXPENSE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Expense Name Field with Dropdown/Autocomplete Suggestions
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = expenseName,
                                onValueChange = { expenseName = it },
                                placeholder = { Text("e.g. Milk, Fuel") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isNameFocused = it.isFocused }
                                    .testTag("expense_name_input"),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }

                        // Amount Field
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            placeholder = { Text("₹0.00") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(100.dp)
                                .testTag("expense_amount_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            )
                        )

                        // Add Button
                        IconButton(
                            onClick = {
                                val amount = amountStr.toDoubleOrNull()
                                if (expenseName.trim().isNotEmpty() && amount != null && amount > 0.0) {
                                    viewModel.addExpense(viewModel.todayStr, expenseName, amount)
                                    expenseName = ""
                                    amountStr = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("add_expense_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Expense",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Clickable Autocomplete Suggestion Chips
                    AnimatedVisibility(visible = filteredSuggestions.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Suggestions:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredSuggestions.forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = {
                                            expenseName = suggestion
                                        },
                                        label = {
                                            Text(
                                                text = suggestion,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        modifier = Modifier.testTag("suggestion_chip_$suggestion")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Entries Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Entries",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "${todayExpenses.size} items",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (todayExpenses.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No expenses added today",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(todayExpenses, key = { it.id }) { expense ->
                ExpenseItemCard(
                    expense = expense,
                    onEditExpense = { exp, newName, newAmt ->
                        viewModel.updateExpense(exp, newName, newAmt)
                    },
                    onDeleteExpense = { exp ->
                        viewModel.deleteExpense(exp)
                    }
                )
            }
        }
    }
}
