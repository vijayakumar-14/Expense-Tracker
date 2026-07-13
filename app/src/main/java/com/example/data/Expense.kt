package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "yyyy-MM-dd"
    val name: String,
    val amount: Double
) {
    companion object {
        fun normalizeName(rawName: String): String {
            val trimmed = rawName.trim()
            if (trimmed.isEmpty()) return ""
            return trimmed.lowercase().replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            }
        }
    }
}
