package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    }

    // Pattern for lakhs/crores (Indian numbering system)
    private val indianFormatter = DecimalFormat("#,##,##0.00", symbols)

    /**
     * Formats an amount using the Indian numbering style with the Rupee symbol.
     * E.g. 100000.00 -> ₹1,00,000.00
     */
    fun format(amount: Double): String {
        return "₹" + indianFormatter.format(amount)
    }

    /**
     * Formats an amount using the Indian numbering style without the Rupee symbol.
     * E.g. 100000.00 -> 1,00,000.00
     */
    fun formatWithoutSymbol(amount: Double): String {
        return indianFormatter.format(amount)
    }
}
