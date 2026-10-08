package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {
    fun formatCurrency(amount: Double, symbol: String = "$"): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return "$symbol ${format.format(amount)}"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
