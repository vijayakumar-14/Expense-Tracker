package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.utils.CurrencyUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyBarChart(
    allExpenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    // Find the start of the current week (Monday) and end of the week (Sunday)
    val today = LocalDate.now()
    val dayOfWeekValue = today.dayOfWeek.value // 1 = Mon, 7 = Sun
    val monday = today.minusDays((dayOfWeekValue - 1).toLong())

    val weekDays = remember(monday) {
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    val dailyTotals = remember(allExpenses, weekDays) {
        weekDays.map { day ->
            allExpenses.filter { it.date == day.toString() }.sumOf { it.amount }
        }
    }

    val maxTotal = remember(dailyTotals) {
        val max = dailyTotals.maxOrNull() ?: 0.0
        if (max == 0.0) 100.0 else max
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_bar_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This Week's Daily Spending",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyTotals.forEachIndexed { index, total ->
                    val dayLabel = weekDays[index].dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val barHeightRatio = (total / maxTotal).toFloat()
                    val animatedRatio by animateFloatAsState(
                        targetValue = barHeightRatio,
                        animationSpec = tween(durationMillis = 800),
                        label = "BarHeight"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (total > 0) {
                            Text(
                                text = "₹${CurrencyUtils.formatWithoutSymbol(total).substringBefore(".")}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(text = "₹0", fontSize = 10.sp, color = Color.Transparent)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Drawing the Bar
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(0.75f * animatedRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (weekDays[index] == today) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.secondary
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (weekDays[index] == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (weekDays[index] == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Color palette for Donut segments
private val DonutColors = listOf(
    Color(0xFF005CB9),
    Color(0xFFADC6FF),
    Color(0xFFFFB4AB),
    Color(0xFF386500),
    Color(0xFF006C51),
    Color(0xFF7F4E00),
    Color(0xFF6750A4),
    Color(0xFF984061)
)

@Composable
fun MonthlyDonutChart(
    groupedExpenses: List<Pair<String, Double>>,
    totalMonthAmount: Double,
    modifier: Modifier = Modifier
) {
    val totalAmount = if (totalMonthAmount <= 0.0) 1.0 else totalMonthAmount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_donut_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Expense Breakdown by Type",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (groupedExpenses.isEmpty() || totalMonthAmount <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded this month",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .testTag("donut_canvas_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            groupedExpenses.forEachIndexed { index, pair ->
                                val sweepAngle = ((pair.second / totalAmount) * 360f).toFloat()
                                val color = DonutColors[index % DonutColors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    size = Size(size.width, size.height),
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Text in center of Donut
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = CurrencyUtils.format(totalMonthAmount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Show top 5 or 6, group others if needed, but for our app showing them all or up to 6 is clean
                        groupedExpenses.take(5).forEachIndexed { index, pair ->
                            val color = DonutColors[index % DonutColors.size]
                            val percent = (pair.second / totalAmount) * 100.0

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pair.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format("%.0f%%", percent),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (groupedExpenses.size > 5) {
                            val otherSum = groupedExpenses.drop(5).sumOf { it.second }
                            val percent = (otherSum / totalAmount) * 100.0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Other",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format("%.0f%%", percent),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedCategoryProgressList(
    groupedExpenses: List<Pair<String, Double>>,
    totalMonthAmount: Double,
    modifier: Modifier = Modifier
) {
    val totalAmount = if (totalMonthAmount <= 0.0) 1.0 else totalMonthAmount

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        groupedExpenses.forEachIndexed { index, pair ->
            val color = DonutColors[index % DonutColors.size]
            val percent = (pair.second / totalAmount).toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pair.first,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = CurrencyUtils.format(pair.second),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = String.format("%.1f%%", percent * 100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
