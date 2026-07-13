package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    allExpenses: List<Expense>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect dates that have expenses to show dots
    val expenseDates = remember(allExpenses) {
        allExpenses.map { it.date }.toSet()
    }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    // dayOfWeek value: 1 = Monday, 7 = Sunday
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val emptySlotsBefore = firstDayOfWeek - 1 // 0 to 6 empty spots

    val daysOfWeekLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    // Gesture detection for swiping months
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(12.dp)
            .pointerInput(currentMonth) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetX > 150f) {
                            // Swipe right -> previous month
                            onMonthChanged(currentMonth.minusMonths(1))
                        } else if (dragOffsetX < -150f) {
                            // Swipe left -> next month
                            onMonthChanged(currentMonth.plusMonths(1))
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                    }
                )
            }
    ) {
        // Month Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMonthChanged(currentMonth.minusMonths(1)) },
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag("calendar_month_title")
            )

            IconButton(
                onClick = { onMonthChanged(currentMonth.plusMonths(1)) },
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeekLabels.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Days Grid
        val totalCells = emptySlotsBefore + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - emptySlotsBefore + 1

                    if (cellIndex < emptySlotsBefore || dayNum > daysInMonth) {
                        // Empty spacer cell
                        Box(modifier = Modifier.size(40.dp))
                    } else {
                        val date = currentMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val hasExpense = expenseDates.contains(date.toString())
                        val isToday = date == LocalDate.now()

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.secondary
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) }
                                .testTag("calendar_day_${date}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                if (hasExpense) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (row < rows - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
