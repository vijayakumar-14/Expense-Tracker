package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseDao
import com.example.utils.CurrencyUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class ExpenseViewModel(private val expenseDao: ExpenseDao) : ViewModel() {

    // Real-time Today's Date
    val today: LocalDate get() = LocalDate.now()
    val todayStr: String get() = LocalDate.now().toString()

    // Selected Date (For Edit Expense tab / Quick Jump)
    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    // Calendar's current visible month & year (for Edit tab calendar)
    private val _calendarMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth

    // Stats tab selected month & year
    private val _statsMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val statsMonth: StateFlow<YearMonth> = _statsMonth

    // Shared Flow to dispatch snackbar notification messages
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Keep track of the last known current month to detect transitions
    private var lastKnownMonth = YearMonth.now()

    /**
     * Checks if the device's current date has transitioned to a new month.
     * If so, automatically updates the active statistics display month to the new current month,
     * resetting active dashboard totals and charts to zero since no entries exist for the new month,
     * while preserving past months' entries.
     */
    fun checkForMonthRollover() {
        val currentMonth = YearMonth.now()
        if (currentMonth != lastKnownMonth) {
            if (_statsMonth.value == lastKnownMonth) {
                _statsMonth.value = currentMonth
            }
            if (_calendarMonth.value == lastKnownMonth) {
                _calendarMonth.value = currentMonth
            }
            lastKnownMonth = currentMonth
        }
    }

    // All expenses from database
    val allExpenses: StateFlow<List<Expense>> = expenseDao.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Autocomplete suggestions: distinct names sorted
    val autocompleteSuggestions: StateFlow<List<String>> = expenseDao.getDistinctExpenseNames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Today's expenses list
    val todayExpenses: StateFlow<List<Expense>> = allExpenses.map { expenses ->
        expenses.filter { it.date == todayStr }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected date's expenses list
    val selectedDateExpenses: StateFlow<List<Expense>> = combine(allExpenses, selectedDate) { expenses, date ->
        expenses.filter { it.date == date.toString() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Database operations
    fun addExpense(date: String, name: String, amount: Double) {
        val normalizedName = Expense.normalizeName(name)
        if (normalizedName.isEmpty() || amount <= 0.0) return
        viewModelScope.launch {
            expenseDao.insertExpense(
                Expense(
                    date = date,
                    name = normalizedName,
                    amount = amount
                )
            )
            _snackbarMessage.tryEmit("Added \"$normalizedName\" of ${CurrencyUtils.format(amount)}")
        }
    }

    fun updateExpense(expense: Expense, name: String, amount: Double) {
        val normalizedName = Expense.normalizeName(name)
        if (normalizedName.isEmpty() || amount <= 0.0) return
        viewModelScope.launch {
            expenseDao.updateExpense(
                expense.copy(
                    name = normalizedName,
                    amount = amount
                )
            )
            _snackbarMessage.tryEmit("Updated to \"$normalizedName\" of ${CurrencyUtils.format(amount)}")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
            _snackbarMessage.tryEmit("Deleted \"${expense.name}\"")
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        // Also update calendar Month if we jumped
        _calendarMonth.value = YearMonth.from(date)
    }

    fun selectCalendarMonth(month: YearMonth) {
        _calendarMonth.value = month
    }

    fun selectStatsMonth(month: YearMonth) {
        _statsMonth.value = month
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    fun importExpenses(expenses: List<Expense>) {
        viewModelScope.launch {
            var importedCount = 0
            for (expense in expenses) {
                val normalizedName = Expense.normalizeName(expense.name)
                if (normalizedName.isNotEmpty() && expense.amount > 0.0) {
                    expenseDao.insertExpense(
                        Expense(
                            date = expense.date,
                            name = normalizedName,
                            amount = expense.amount
                        )
                    )
                    importedCount++
                }
            }
            _snackbarMessage.tryEmit("Imported $importedCount expenses successfully")
        }
    }

    // Helper to calculate total for a specific date
    fun getTotalForDate(date: String, expenses: List<Expense>): Double {
        return expenses.filter { it.date == date }.sumOf { it.amount }
    }

    // Class Factory to instantiate ViewModel with DAO
    class Factory(private val expenseDao: ExpenseDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                return ExpenseViewModel(expenseDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
