package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.ExpenseDatabase
import com.example.ui.ExpenseViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local SQLite Room database and DAO
        val database = ExpenseDatabase.getDatabase(applicationContext)
        val expenseDao = database.expenseDao
        
        // Load ViewModel using our custom Factory
        val viewModel: ExpenseViewModel by viewModels {
            ExpenseViewModel.Factory(expenseDao)
        }

        enableEdgeToEdge()
        setContent {
            // Track theme state (Light/Dark mode)
            val systemTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemTheme) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
