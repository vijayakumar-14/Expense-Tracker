package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.components.CalendarView
import com.example.ui.components.ExpenseItemCard
import com.example.utils.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditExpenseTab(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val selectedDateExpenses by viewModel.selectedDateExpenses.collectAsState()
    val autocompleteSuggestions by viewModel.autocompleteSuggestions.collectAsState()

    var editExpenseName by remember { mutableStateOf("") }
    var editAmountStr by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    val formattedSelectedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
    }

    val selectedDateTotal = remember(selectedDateExpenses) {
        selectedDateExpenses.sumOf { it.amount }
    }

    val filteredSuggestions = remember(editExpenseName, autocompleteSuggestions) {
        if (editExpenseName.isBlank()) {
            autocompleteSuggestions.take(5)
        } else {
            autocompleteSuggestions
                .filter { it.contains(editExpenseName, ignoreCase = true) }
                .sortedWith(compareBy<String> {
                    if (it.equals(editExpenseName, ignoreCase = true)) 0 else 1
                }.thenBy {
                    if (it.startsWith(editExpenseName, ignoreCase = true)) 0 else 1
                }.thenBy {
                    it.length
                }.thenBy {
                    it.lowercase()
                })
                .take(5)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("edit_expense_tab_column")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HISTORICAL ENTRIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Browse by Calendar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Custom Swipeable Month Calendar
        item {
            CalendarView(
                currentMonth = calendarMonth,
                selectedDate = selectedDate,
                allExpenses = allExpenses,
                onDateSelected = { date ->
                    viewModel.selectDate(date)
                },
                onMonthChanged = { month ->
                    viewModel.selectCalendarMonth(month)
                },
                modifier = Modifier.testTag("historical_calendar_view")
            )
        }

        // Selected Date Header Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_date_total_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedSelectedDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Daily Total",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = CurrencyUtils.format(selectedDateTotal),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Form to add entries to selected date specifically
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_historical_expense_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ADD EXPENSE FOR ${selectedDate}",
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
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = editExpenseName,
                                onValueChange = { editExpenseName = it },
                                placeholder = { Text("e.g. Milk, Fuel") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_tab_name_input"),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }

                        OutlinedTextField(
                            value = editAmountStr,
                            onValueChange = { editAmountStr = it },
                            placeholder = { Text("₹0.00") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(100.dp)
                                .testTag("edit_tab_amount_input"),
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

                        IconButton(
                            onClick = {
                                val amount = editAmountStr.toDoubleOrNull()
                                if (editExpenseName.trim().isNotEmpty() && amount != null && amount > 0.0) {
                                    viewModel.addExpense(selectedDate.toString(), editExpenseName, amount)
                                    editExpenseName = ""
                                    editAmountStr = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("edit_tab_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Expense",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Autocomplete Chips
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
                                            editExpenseName = suggestion
                                        },
                                        label = {
                                            Text(
                                                text = suggestion,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        modifier = Modifier.testTag("edit_tab_suggestion_chip_$suggestion")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logged Entries",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "${selectedDateExpenses.size} items",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Entries
        if (selectedDateExpenses.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
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
                        text = "No expenses logged for this date",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(selectedDateExpenses, key = { it.id }) { expense ->
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
