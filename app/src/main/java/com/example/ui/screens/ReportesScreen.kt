package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun ReportesScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val proveedores by viewModel.proveedoresConSaldo.collectAsState()
    val transacciones by viewModel.transacciones.collectAsState()
    val compras by viewModel.compras.collectAsState()
    val pagosProveedor by viewModel.pagosProveedor.collectAsState()

    // Calculations
    val ventasContado = remember(transacciones) {
        transacciones.filter { it.tipo == "VENTA_CONTADO" }.sumOf { it.montoTotal }
    }
    val cobrosAbonos = remember(transacciones) {
        transacciones.filter { it.tipo == "ABONO" }.sumOf { it.montoTotal }
    }
    val ventasCredito = remember(transacciones) {
        transacciones.filter { it.tipo == "COMPRA" }.sumOf { it.montoTotal }
    }
    val totalIngresosEfectivos = ventasContado + cobrosAbonos

    val comprasContado = remember(compras) {
        compras.filter { it.condicion == "CONTADO" }.sumOf { it.montoTotal }
    }
    val pagosAProveedores = remember(pagosProveedor) {
        pagosProveedor.sumOf { it.monto }
    }
    val totalEgresosEfectivos = comprasContado + pagosAProveedores

    val utilidadEstimada = totalIngresosEfectivos - totalEgresosEfectivos

    val totalCxC = remember(clientes) { clientes.sumOf { it.saldoPendiente } }
    val totalCxp = remember(proveedores) { proveedores.sumOf { it.saldoPendienteCXP } }
    val balanceCredito = totalCxC - totalCxp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reportes y Estadísticas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Balance operativo de ${config.nombreComercio}", fontSize = 12.sp, color = SoftGray)
                }
                IconButton(
                    onClick = {
                        val resumenTexto = """
📊 REPORTE FINANCIERO - ${config.nombreComercio}
----------------------------------------
INGRESOS EFECTIVOS: ${FormatUtils.formatCurrency(totalIngresosEfectivos, config.monedaSimbolo)}
• Ventas al Contado: ${FormatUtils.formatCurrency(ventasContado, config.monedaSimbolo)}
• Cobros / Abonos CXC: ${FormatUtils.formatCurrency(cobrosAbonos, config.monedaSimbolo)}

EGRESOS EFECTIVOS: ${FormatUtils.formatCurrency(totalEgresosEfectivos, config.monedaSimbolo)}
• Compras al Contado: ${FormatUtils.formatCurrency(comprasContado, config.monedaSimbolo)}
• Pagos a Proveedores: ${FormatUtils.formatCurrency(pagosAProveedores, config.monedaSimbolo)}

UTILIDAD ESTIMADA: ${FormatUtils.formatCurrency(utilidadEstimada, config.monedaSimbolo)}
----------------------------------------
POSICIÓN DE CRÉDITO:
• Cuentas por Cobrar (CXC): ${FormatUtils.formatCurrency(totalCxC, config.monedaSimbolo)}
• Cuentas por Pagar (CXP): ${FormatUtils.formatCurrency(totalCxp, config.monedaSimbolo)}
• Balance Neto: ${FormatUtils.formatCurrency(balanceCredito, config.monedaSimbolo)}
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, resumenTexto)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir Reporte"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir reporte", tint = CafeBrown)
                }
            }
        }

        // Operative Net Profit Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (utilidadEstimada >= 0) CafeDarkBrown else SoftRed
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Flujo de Caja Neto (Ingresos - Gastos)", fontSize = 12.sp, color = SmoothBeige.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatCurrency(utilidadEstimada, config.monedaSimbolo),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (utilidadEstimada >= 0) GoldenCrema else LightText
                    )
                    Text(
                        text = "Equivalente: ${FormatUtils.formatBs(utilidadEstimada, config.tasaCambioBs)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (utilidadEstimada >= 0) SmoothBeige else LightText.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ingresos Reales:", fontSize = 11.sp, color = SoftGray)
                            Text(FormatUtils.formatCurrency(totalIngresosEfectivos, config.monedaSimbolo), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SoftGreen)
                            Text(FormatUtils.formatBs(totalIngresosEfectivos, config.tasaCambioBs), fontSize = 11.sp, color = SoftGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Egresos Reales:", fontSize = 11.sp, color = SoftGray)
                            Text(FormatUtils.formatCurrency(totalEgresosEfectivos, config.monedaSimbolo), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                            Text(FormatUtils.formatBs(totalEgresosEfectivos, config.tasaCambioBs), fontSize = 11.sp, color = SoftRed)
                        }
                    }
                }
            }
        }

        // Detailed Breakdown Card
        item {
            Text("Desglose de Ingresos y Gastos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportRowItem(label = "Ventas Mostrador / Contado", value = FormatUtils.formatDual(ventasContado, config.tasaCambioBs, config.monedaSimbolo), color = SoftGreen)
                    ReportRowItem(label = "Cobros de Deudas (Abonos)", value = FormatUtils.formatDual(cobrosAbonos, config.tasaCambioBs, config.monedaSimbolo), color = SoftGreen)
                    ReportRowItem(label = "Ventas a Crédito otorgadas", value = FormatUtils.formatDual(ventasCredito, config.tasaCambioBs, config.monedaSimbolo), color = CafeBrown)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ReportRowItem(label = "Compras de Insumos al Contado", value = FormatUtils.formatDual(comprasContado, config.tasaCambioBs, config.monedaSimbolo), color = SoftRed)
                    ReportRowItem(label = "Pagos Realizados a Proveedores", value = FormatUtils.formatDual(pagosAProveedores, config.tasaCambioBs, config.monedaSimbolo), color = SoftRed)
                }
            }
        }

        // Credit Balance Card (CXC vs CXP)
        item {
            Text("Balance de Cuentas (CXC vs CXP)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportRowItem(
                        label = "Cuentas por Cobrar (A favor)",
                        value = FormatUtils.formatDual(totalCxC, config.tasaCambioBs, config.monedaSimbolo),
                        color = SoftGreen
                    )
                    ReportRowItem(
                        label = "Cuentas por Pagar (A proveedores)",
                        value = FormatUtils.formatDual(totalCxp, config.tasaCambioBs, config.monedaSimbolo),
                        color = SoftRed
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ReportRowItem(
                        label = "Posición Neta de Crédito",
                        value = FormatUtils.formatDual(balanceCredito, config.tasaCambioBs, config.monedaSimbolo),
                        color = if (balanceCredito >= 0) SoftGreen else SoftRed,
                        isBold = true
                    )
                }
            }
        }

        // Top Debtors and Clients
        item {
            Text("Principales Cuentas por Cobrar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val topDeudores = clientes.filter { it.saldoPendiente > 0 }.sortedByDescending { it.saldoPendiente }.take(5)
            if (topDeudores.isEmpty()) {
                Text("No hay clientes con saldo pendiente de pago.", color = SoftGray, fontSize = 12.sp)
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        topDeudores.forEach { d ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(d.cliente.nombre, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    FormatUtils.formatCurrency(d.saldoPendiente, config.monedaSimbolo),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftRed
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
private fun ReportRowItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
