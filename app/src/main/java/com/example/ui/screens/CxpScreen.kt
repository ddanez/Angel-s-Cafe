package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PagoProveedor
import com.example.data.repository.ProveedorConSaldo
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun CxpScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.configuracion.collectAsState()
    val proveedores by viewModel.proveedoresConSaldo.collectAsState()
    val pagosProveedor by viewModel.pagosProveedor.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var provParaPago by remember { mutableStateOf<ProveedorConSaldo?>(null) }
    var tabSeleccionada by remember { mutableIntStateOf(0) } // 0: Deudas con Proveedores, 1: Historial de Pagos

    val provDeudores = remember(proveedores) {
        proveedores.filter { it.saldoPendienteCXP > 0.0 }.sortedByDescending { it.saldoPendienteCXP }
    }
    val totalCxp = remember(provDeudores) {
        provDeudores.sumOf { it.saldoPendienteCXP }
    }

    val filtrados = remember(searchQuery, provDeudores) {
        if (searchQuery.isBlank()) provDeudores
        else provDeudores.filter {
            it.proveedor.nombre.contains(searchQuery, ignoreCase = true) ||
            it.proveedor.empresa.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // CXP Hero Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cuentas por Pagar (CXP)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                    Text("📑 Compromisos a Proveedores", fontSize = 12.sp, color = CafeBrown)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FormatUtils.formatCurrency(totalCxp, config.monedaSimbolo),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftRed
                )
                Text(
                    text = "Equivalente: ${FormatUtils.formatBs(totalCxp, config.tasaCambioBs)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CafeDarkBrown
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${provDeudores.size} proveedores pendientes de pago",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = tabSeleccionada,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = tabSeleccionada == 0,
                onClick = { tabSeleccionada = 0 },
                text = { Text("Por Pagar (${provDeudores.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = tabSeleccionada == 1,
                onClick = { tabSeleccionada = 1 },
                text = { Text("Pagos Realizados", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tabSeleccionada == 0) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar proveedor o empresa...", color = SoftGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CafeBrown) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cxp_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filtrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤝", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "¡Excelente! No tienes deudas pendientes con proveedores." else "Sin resultados.",
                            color = SoftGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtrados) { item ->
                        val prov = item.proveedor
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prov.nombre,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (prov.empresa.isNotBlank()) {
                                            Text(prov.empresa, fontSize = 12.sp, color = CafeBrown)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Saldo por pagar:", fontSize = 10.sp, color = SoftRed)
                                        Text(
                                            text = FormatUtils.formatCurrency(item.saldoPendienteCXP, config.monedaSimbolo),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SoftRed
                                        )
                                        Text(
                                            text = FormatUtils.formatBs(item.saldoPendienteCXP, config.tasaCambioBs),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Crédito: ${FormatUtils.formatCurrency(item.totalComprasCredito, config.monedaSimbolo)}",
                                        fontSize = 11.sp,
                                        color = SoftGray
                                    )
                                    Text(
                                        text = "Pagado: ${FormatUtils.formatDual(item.totalPagos, config.tasaCambioBs, config.monedaSimbolo)}",
                                        fontSize = 11.sp,
                                        color = SoftGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { provParaPago = item },
                                        colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Pagar a Proveedor 💵", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Historial de pagos a proveedores
            if (pagosProveedor.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay pagos a proveedores registrados.", color = SoftGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pagosProveedor) { pago ->
                        val provNombre = remember(pago.proveedorId, proveedores) {
                            proveedores.find { it.proveedor.id == pago.proveedorId }?.proveedor?.nombre ?: "Proveedor General"
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(provNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(pago.detalle, fontSize = 12.sp, color = SoftGray)
                                    Text(FormatUtils.formatDate(pago.fecha), fontSize = 10.sp, color = SoftGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FormatUtils.formatCurrency(pago.monto, config.monedaSimbolo),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SoftGreen
                                        )
                                        Text(
                                            text = FormatUtils.formatBs(pago.monto, config.tasaCambioBs),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = SoftGreen.copy(alpha = 0.85f)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.borrarPagoProveedor(pago) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SoftRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para registrar pago al proveedor
    provParaPago?.let { item ->
        RegistrarPagoProvDialog(
            proveedorNombre = item.proveedor.nombre,
            saldoActual = item.saldoPendienteCXP,
            moneda = config.monedaSimbolo,
            onDismiss = { provParaPago = null },
            onConfirm = { monto, detalle ->
                viewModel.registrarPagoProveedor(item.proveedor.id, monto, detalle)
                provParaPago = null
            }
        )
    }
}

@Composable
private fun RegistrarPagoProvDialog(
    proveedorNombre: String,
    saldoActual: Double,
    moneda: String,
    onDismiss: () -> Unit,
    onConfirm: (monto: Double, detalle: String) -> Unit
) {
    var montoStr by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("Pago transferencia / efectivo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pago a Proveedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Proveedor: $proveedorNombre", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Deuda actual: ${FormatUtils.formatCurrency(saldoActual, moneda)}", color = SoftRed, fontSize = 12.sp)

                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto a Pagar ($moneda) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row {
                    TextButton(onClick = { montoStr = saldoActual.toString() }) {
                        Text("Pagar totalidad (${FormatUtils.formatCurrency(saldoActual, moneda)})", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it },
                    label = { Text("Detalle o referencia del pago") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montoStr.toDoubleOrNull() ?: 0.0
                    if (m > 0) onConfirm(m, detalle)
                },
                enabled = (montoStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Confirmar Pago")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
