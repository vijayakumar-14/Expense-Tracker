package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.ExpenseViewModel
import com.example.ui.components.MonthlyDonutChart
import com.example.ui.components.WeeklyBarChart
import com.example.ui.components.GroupedCategoryProgressList
import com.example.utils.PdfExporter
import com.example.utils.CurrencyUtils
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ExpenseStatsTab(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    val statsMonth by viewModel.statsMonth.collectAsState()

    var showMonthDropdown by remember { mutableStateOf(false) }

    // Generate months from database distinct months (past and future), always including current month
    val monthOptions = remember(allExpenses) {
        val monthsFromDb = allExpenses.mapNotNull { expense ->
            runCatching {
                if (expense.date.length >= 7) {
                    YearMonth.parse(expense.date.substring(0, 7))
                } else {
                    null
                }
            }.getOrNull()
        }.toSet()

        val current = YearMonth.now()
        (monthsFromDb + current).sortedDescending()
    }

    // Calculations
    val todayStr = viewModel.todayStr
    val todayTotal = remember(allExpenses, todayStr) {
        allExpenses.filter { it.date == todayStr }.sumOf { it.amount }
    }

    // Week calculation (Monday to Sunday)
    val today = LocalDate.now()
    val dayOfWeekValue = today.dayOfWeek.value
    val monday = today.minusDays((dayOfWeekValue - 1).toLong())
    val sunday = monday.plusDays(6)
    val mondayStr = monday.toString()
    val sundayStr = sunday.toString()

    val weekTotal = remember(allExpenses, mondayStr, sundayStr) {
        allExpenses.filter { it.date in mondayStr..sundayStr }.sumOf { it.amount }
    }

    // Selected Month totals
    val statsMonthPrefix = remember(statsMonth) {
        statsMonth.toString() // e.g. "2026-07"
    }

    val selectedMonthExpenses = remember(allExpenses, statsMonthPrefix) {
        allExpenses.filter { it.date.startsWith(statsMonthPrefix) }
    }

    val selectedMonthTotal = remember(selectedMonthExpenses) {
        selectedMonthExpenses.sumOf { it.amount }
    }

    // Group selected month expenses by capitalized name
    val groupedMonthExpenses = remember(selectedMonthExpenses) {
        selectedMonthExpenses
            .groupBy { it.name }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val formattedStatsMonth = remember(statsMonth) {
        statsMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("expense_stats_tab_column")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Header & Month Selector Dropdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ANALYTICS & SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Expense Statistics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Dropdown trigger
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { showMonthDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("stats_month_selector"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formattedStatsMonth,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (showMonthDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { showMonthDropdown = false },
                        modifier = Modifier
                            .testTag("stats_month_dropdown_menu")
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        monthOptions.forEach { month ->
                            val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                            DropdownMenuItem(
                                text = { Text(text = label, fontSize = 13.sp) },
                                onClick = {
                                    viewModel.selectStatsMonth(month)
                                    showMonthDropdown = false
                                },
                                modifier = Modifier.testTag("dropdown_item_${month}")
                            )
                        }
                    }
                }
            }
        }

        // Top Financial Metrics Dashboard Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Today",
                    value = CurrencyUtils.format(todayTotal),
                    icon = Icons.Default.Today,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_today")
                )
                StatCard(
                    title = "This Week",
                    value = CurrencyUtils.format(weekTotal),
                    icon = Icons.Default.DateRange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_week")
                )
                StatCard(
                    title = statsMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    value = CurrencyUtils.format(selectedMonthTotal),
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_month")
                )
            }
        }

        // PDF Export Options Section
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_export_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "EXPORT REPORTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download PDF Statement",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val todayExpenses = allExpenses.filter { it.date == todayStr }
                                PdfExporter.exportExpensesToPdf(
                                    context = context,
                                    title = "Daily Expense Report",
                                    dateRange = todayStr,
                                    expenses = todayExpenses,
                                    totalAmount = todayTotal
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_today_button"),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Export Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                val weekExpenses = allExpenses.filter { it.date in mondayStr..sundayStr }
                                PdfExporter.exportExpensesToPdf(
                                    context = context,
                                    title = "Weekly Expense Report",
                                    dateRange = "$mondayStr to $sundayStr",
                                    expenses = weekExpenses,
                                    totalAmount = weekTotal
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_week_button"),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Export This Week",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                PdfExporter.exportExpensesToPdf(
                                    context = context,
                                    title = "Monthly Expense Report (${formattedStatsMonth})",
                                    dateRange = "${statsMonth.year}-${String.format("%02d", statsMonth.monthValue)}",
                                    expenses = selectedMonthExpenses,
                                    totalAmount = selectedMonthTotal
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_month_button"),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Export This Month",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Weekly Bar Chart
        item {
            WeeklyBarChart(
                allExpenses = allExpenses,
                modifier = Modifier.testTag("weekly_bar_chart")
            )
        }

        // Donut Chart Segment
        item {
            MonthlyDonutChart(
                groupedExpenses = groupedMonthExpenses,
                totalMonthAmount = selectedMonthTotal,
                modifier = Modifier.testTag("monthly_donut_chart")
            )
        }

        // Detailed List Header
        if (groupedMonthExpenses.isNotEmpty()) {
            item {
                Text(
                    text = "Category Details (${statsMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                GroupedCategoryProgressList(
                    groupedExpenses = groupedMonthExpenses,
                    totalMonthAmount = selectedMonthTotal,
                    modifier = Modifier.testTag("category_progress_list")
                )
            }
        } else {
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
                        text = "No expenses recorded for $formattedStatsMonth yet",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("empty_stats_message")
                    )
                }
            }
        }

        // Query & Footer Section
        item {
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:vijayakumar.arunachalam2007@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Support Query")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("raise_query_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Raise a Query",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Designed and created by Vijaya Kumar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("footer_author_text")
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1
            )
        }
    }
}
