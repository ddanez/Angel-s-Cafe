package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Formatea una cantidad con su símbolo primario (generalmente en USD $)
     */
    fun formatCurrency(amount: Double, symbol: String = "$"): String {
        return "$symbol ${numberFormat.format(amount)}"
    }

    /**
     * Formatea el equivalente en Bolívares usando la tasa de cambio
     */
    fun formatBs(amountUsd: Double, tasaCambioBs: Double): String {
        val totalBs = amountUsd * if (tasaCambioBs > 0) tasaCambioBs else 875.65
        return "Bs. ${numberFormat.format(totalBs)}"
    }

    /**
     * Muestra ambos precios juntos: "$ 10.00 | Bs. 545.00"
     */
    fun formatDual(amountUsd: Double, tasaCambioBs: Double, symbol: String = "$"): String {
        val usdStr = formatCurrency(amountUsd, symbol)
        val bsStr = formatBs(amountUsd, tasaCambioBs)
        return "$usdStr • $bsStr"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
