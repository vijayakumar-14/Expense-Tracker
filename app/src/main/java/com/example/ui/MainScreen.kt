package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CalendarView
import com.example.ui.screens.EditExpenseTab
import com.example.ui.screens.ExpenseStatsTab
import com.example.ui.screens.TodayExpenseTab
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.Expense
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// Type-safe Route constants
object Routes {
    const val TODAY_EXPENSE = "today_expense"
    const val EDIT_EXPENSE = "edit_expense"
    const val EXPENSE_STATS = "expense_stats"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val allExpenses by viewModel.allExpenses.collectAsState()

    // Register ActivityResult launchers for Backup (Export) & Restore (Import)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val jsonArray = JSONArray()
                        for (expense in allExpenses) {
                            val obj = JSONObject().apply {
                                put("date", expense.date)
                                put("name", expense.name)
                                put("amount", expense.amount)
                            }
                            jsonArray.put(obj)
                        }
                        outputStream.write(jsonArray.toString(2).toByteArray())
                    }
                    viewModel.showSnackbar("Backup exported successfully")
                } catch (e: Exception) {
                    viewModel.showSnackbar("Failed to export backup: ${e.localizedMessage}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(it)?.use { inputStream ->
                        inputStream.bufferedReader().use { reader -> reader.readText() }
                    }
                    if (!jsonString.isNullOrEmpty()) {
                        val jsonArray = JSONArray(jsonString)
                        val importedExpenses = mutableListOf<Expense>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val date = obj.getString("date")
                            val name = obj.getString("name")
                            val amount = obj.getDouble("amount")
                            importedExpenses.add(
                                Expense(
                                    date = date,
                                    name = name,
                                    amount = amount
                                )
                            )
                        }
                        if (importedExpenses.isNotEmpty()) {
                            viewModel.importExpenses(importedExpenses)
                        } else {
                            viewModel.showSnackbar("No valid expense records found in backup")
                        }
                    } else {
                        viewModel.showSnackbar("Selected backup file is empty")
                    }
                } catch (e: Exception) {
                    viewModel.showSnackbar("Failed to import backup: ${e.localizedMessage}")
                }
            }
        }
    }

    // Collect snackbar messages from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }

    // Standalone state for drawer calendar to let users browse independently
    val selectedDate by viewModel.selectedDate.collectAsState()
    var drawerMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.from(selectedDate))
    }

    // Determine current active navigation route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.TODAY_EXPENSE

    // Automatically check for month rollover when active route changes or on first composition
    LaunchedEffect(currentRoute) {
        viewModel.checkForMonthRollover()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("navigation_drawer_sheet"),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quick Date Jump",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Text(
                        text = "Tap any day to jump directly to its logged entries and add or edit items.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar in the navigation drawer
                    CalendarView(
                        currentMonth = drawerMonth,
                        selectedDate = selectedDate,
                        allExpenses = allExpenses,
                        onDateSelected = { clickedDate ->
                            viewModel.selectDate(clickedDate)
                            // Close drawer
                            scope.launch { drawerState.close() }
                            // Jump to historical Edit Tab
                            if (currentRoute != Routes.EDIT_EXPENSE) {
                                navController.navigate(Routes.EDIT_EXPENSE) {
                                    popUpTo(Routes.TODAY_EXPENSE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onMonthChanged = { newMonth ->
                            drawerMonth = newMonth
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_calendar_view")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = "Backup & Restore",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Export Button
                        FilledTonalButton(
                            onClick = {
                                try {
                                    exportLauncher.launch("expense_backup_${LocalDate.now()}.json")
                                } catch (e: Exception) {
                                    viewModel.showSnackbar("Cannot launch file exporter: ${e.localizedMessage}")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button"),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Import Button
                        FilledTonalButton(
                            onClick = {
                                try {
                                    importLauncher.launch("application/json")
                                } catch (e: Exception) {
                                    viewModel.showSnackbar("Cannot launch file importer: ${e.localizedMessage}")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_button"),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Helpful hint at footer
                    Text(
                        text = "Currently viewing: $selectedDate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("drawer_selected_date_indicator")
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.testTag("snackbar_host")) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Expense Tracker",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Drawer",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onThemeToggle,
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Light/Dark Theme",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.testTag("main_top_app_bar")
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .height(72.dp)
                        .testTag("main_bottom_nav_bar")
                ) {
                    // Left Tab: Edit Expense
                    NavigationBarItem(
                        selected = currentRoute == Routes.EDIT_EXPENSE,
                        onClick = {
                            if (currentRoute != Routes.EDIT_EXPENSE) {
                                navController.navigate(Routes.EDIT_EXPENSE) {
                                    popUpTo(Routes.TODAY_EXPENSE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Edit Expense"
                            )
                        },
                        label = { Text("Edit", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("nav_tab_edit")
                    )

                    // Center Tab: Today Expense (Default Landing)
                    NavigationBarItem(
                        selected = currentRoute == Routes.TODAY_EXPENSE,
                        onClick = {
                            if (currentRoute != Routes.TODAY_EXPENSE) {
                                navController.navigate(Routes.TODAY_EXPENSE) {
                                    popUpTo(Routes.TODAY_EXPENSE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Today Expense"
                            )
                        },
                        label = { Text("Today", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("nav_tab_today")
                    )

                    // Right Tab: Expense Stats
                    NavigationBarItem(
                        selected = currentRoute == Routes.EXPENSE_STATS,
                        onClick = {
                            if (currentRoute != Routes.EXPENSE_STATS) {
                                navController.navigate(Routes.EXPENSE_STATS) {
                                    popUpTo(Routes.TODAY_EXPENSE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Expense Stats"
                            )
                        },
                        label = { Text("Stats", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("nav_tab_stats")
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY_EXPENSE,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.TODAY_EXPENSE) {
                    TodayExpenseTab(viewModel = viewModel)
                }
                composable(Routes.EDIT_EXPENSE) {
                    EditExpenseTab(viewModel = viewModel)
                }
                composable(Routes.EXPENSE_STATS) {
                    ExpenseStatsTab(viewModel = viewModel)
                }
            }
        }
    }
}
