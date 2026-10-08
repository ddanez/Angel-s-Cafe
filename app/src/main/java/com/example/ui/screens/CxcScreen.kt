package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ClienteConSaldo
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun CxcScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val transacciones by viewModel.transacciones.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var clienteParaAbono by remember { mutableStateOf<ClienteConSaldo?>(null) }
    var tabSeleccionada by remember { mutableIntStateOf(0) } // 0: Clientes Deudores, 1: Historial Abonos

    val deudores = remember(clientes) {
        clientes.filter { it.saldoPendiente > 0.0 }.sortedByDescending { it.saldoPendiente }
    }
    val totalCxC = remember(deudores) {
        deudores.sumOf { it.saldoPendiente }
    }

    val deudoresFiltrados = remember(searchQuery, deudores) {
        if (searchQuery.isBlank()) deudores
        else deudores.filter {
            it.cliente.nombre.contains(searchQuery, ignoreCase = true) ||
            it.cliente.telefono.contains(searchQuery)
        }
    }

    val historialAbonos = remember(transacciones) {
        transacciones.filter { it.tipo == "ABONO" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // CXC Hero Banner
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
                    Text("Cuentas por Cobrar (CXC)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                    Text("💰 Cartera Activa", fontSize = 12.sp, color = CafeBrown)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FormatUtils.formatCurrency(totalCxC, config.monedaSimbolo),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${deudores.size} clientes con saldo pendiente de pago",
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
                text = { Text("Deudores (${deudores.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = tabSeleccionada == 1,
                onClick = { tabSeleccionada = 1 },
                text = { Text("Historial de Abonos", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tabSeleccionada == 0) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar deudor por nombre...", color = SoftGray) },
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
                    .testTag("cxc_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (deudoresFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "¡Excelente! No hay clientes con deudas pendientes." else "Sin resultados.",
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
                    items(deudoresFiltrados) { item ->
                        DeudorCxcCard(
                            item = item,
                            moneda = config.monedaSimbolo,
                            mensajePlantilla = config.mensajeCobro,
                            onRegistrarAbono = { clienteParaAbono = item },
                            onCobrarWhatsApp = { phone, msg ->
                                val clean = phone.replace(Regex("[^0-9+]"), "").replace("+", "")
                                try {
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(msg)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) {}
                            },
                            onCall = { tel ->
                                if (tel.isNotBlank()) {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Historial de abonos
            if (historialAbonos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se han registrado abonos todavía.", color = SoftGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historialAbonos) { abono ->
                        val cliNombre = remember(abono.clienteId, clientes) {
                            clientes.find { it.cliente.id == abono.clienteId }?.cliente?.nombre ?: "Cliente"
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
                                    Text(cliNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(abono.detalle, fontSize = 12.sp, color = SoftGray)
                                    Text(FormatUtils.formatDate(abono.fecha), fontSize = 10.sp, color = SoftGray)
                                }
                                Text(
                                    text = "+ ${FormatUtils.formatCurrency(abono.montoTotal, config.monedaSimbolo)}",
                                    color = SoftGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para registrar abono
    clienteParaAbono?.let { item ->
        RegistrarAbonoCxcDialog(
            clienteNombre = item.cliente.nombre,
            saldoActual = item.saldoPendiente,
            moneda = config.monedaSimbolo,
            onDismiss = { clienteParaAbono = null },
            onConfirm = { monto, detalle ->
                viewModel.registrarVentaO_AbonoManual(item.cliente.id, monto, detalle, "ABONO")
                clienteParaAbono = null
            }
        )
    }
}

@Composable
private fun DeudorCxcCard(
    item: ClienteConSaldo,
    moneda: String,
    mensajePlantilla: String,
    onRegistrarAbono: () -> Unit,
    onCobrarWhatsApp: (String, String) -> Unit,
    onCall: (String) -> Unit
) {
    val cli = item.cliente
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
                        text = cli.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (cli.telefono.isNotBlank()) {
                        Text(text = "📞 ${cli.telefono}", fontSize = 12.sp, color = SoftGray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Debe:", fontSize = 10.sp, color = SoftRed)
                    Text(
                        text = FormatUtils.formatCurrency(item.saldoPendiente, moneda),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SoftRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cli.telefono.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                val msg = mensajePlantilla.replace("%MONTO%", FormatUtils.formatCurrency(item.saldoPendiente, moneda))
                                onCobrarWhatsApp(cli.telefono, msg)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SoftGreen.copy(alpha = 0.15f), contentColor = SoftGreen)
                        ) {
                            Text("Cobrar WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { onCall(cli.telefono) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = CafeBrown)
                        }
                    }
                }

                Button(
                    onClick = onRegistrarAbono,
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+ Abonar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RegistrarAbonoCxcDialog(
    clienteNombre: String,
    saldoActual: Double,
    moneda: String,
    onDismiss: () -> Unit,
    onConfirm: (monto: Double, detalle: String) -> Unit
) {
    var montoStr by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("Abono de deuda a cuenta") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Abono / Pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cliente: $clienteNombre", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Deuda actual: ${FormatUtils.formatCurrency(saldoActual, moneda)}", color = SoftRed, fontSize = 12.sp)

                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto a Abonar ($moneda) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Botón rápido para abonar la totalidad
                Row {
                    TextButton(onClick = { montoStr = saldoActual.toString() }) {
                        Text("Abonar totalidad (${FormatUtils.formatCurrency(saldoActual, moneda)})", fontSize = 11.sp)
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
                Text("Confirmar Abono")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
