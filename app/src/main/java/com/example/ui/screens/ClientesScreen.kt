package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.Cliente
import com.example.data.model.Transaccion
import com.example.data.repository.ClienteConSaldo
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun ClientesScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val selectedClienteId by viewModel.selectedClienteId.collectAsState()
    val transaccionesCliente by viewModel.transaccionesDelClienteSeleccionado.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddClienteDialog by remember { mutableStateOf(false) }
    var showManualTxDialog by remember { mutableStateOf<String?>(null) } // "COMPRA" o "ABONO"
    var clienteToDelete by remember { mutableStateOf<Cliente?>(null) }

    val selectedCliente = remember(selectedClienteId, clientes) {
        clientes.find { it.cliente.id == selectedClienteId }
    }

    // Handle back button when client detail is open
    BackHandler(enabled = selectedCliente != null) {
        viewModel.selectCliente(null)
    }

    if (selectedCliente != null) {
        // --- DETALLE DE CUENTA DEL CLIENTE ---
        ClienteDetalleView(
            clienteConSaldo = selectedCliente,
            transacciones = transaccionesCliente,
            moneda = config.monedaSimbolo,
            nombreComercio = config.nombreComercio,
            mensajeCobro = config.mensajeCobro,
            onBack = { viewModel.selectCliente(null) },
            onRegistrarTx = { tipo -> showManualTxDialog = tipo },
            onDeleteTx = { tx -> viewModel.borrarTransaccion(tx) },
            onDeleteCliente = { clienteToDelete = selectedCliente.cliente }
        )
    } else {
        // --- LISTADO DE CLIENTES ---
        val filteredClientes = remember(searchQuery, clientes) {
            if (searchQuery.isBlank()) clientes
            else clientes.filter {
                it.cliente.nombre.contains(searchQuery, ignoreCase = true) ||
                it.cliente.telefono.contains(searchQuery)
            }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddClienteDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_cliente_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Cliente")
                }
            },
            modifier = modifier
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Summary Card
                val totalCartera = remember(clientes) { clientes.sumOf { it.saldoPendiente } }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Directorio de Clientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${clientes.size} clientes registrados", fontSize = 12.sp, color = SoftGray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo en Cartera (CXC)", fontSize = 11.sp, color = SoftRed)
                            Text(
                                text = FormatUtils.formatCurrency(totalCartera, config.monedaSimbolo),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar cliente por nombre o teléfono...", color = SoftGray) },
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
                        .testTag("search_cliente_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredClientes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👥", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No hay clientes registrados aún." else "Sin resultados.",
                                color = SoftGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredClientes) { item ->
                            ClienteCardItem(
                                item = item,
                                moneda = config.monedaSimbolo,
                                onClick = { viewModel.selectCliente(item.cliente.id) },
                                onCall = { tel ->
                                    if (tel.isNotBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal para registrar abono o compra manual
    showManualTxDialog?.let { tipo ->
        selectedCliente?.let { item ->
            ManualTxDialog(
                tipo = tipo,
                clienteNombre = item.cliente.nombre,
                moneda = config.monedaSimbolo,
                onDismiss = { showManualTxDialog = null },
                onConfirm = { monto, detalle ->
                    viewModel.registrarVentaO_AbonoManual(item.cliente.id, monto, detalle, tipo)
                    showManualTxDialog = null
                }
            )
        }
    }

    // Modal nuevo cliente
    if (showAddClienteDialog) {
        NuevoClienteDialog(
            onDismiss = { showAddClienteDialog = false },
            onConfirm = { nombre, tel, dir ->
                viewModel.agregarCliente(nombre, tel, dir)
                showAddClienteDialog = false
            }
        )
    }

    // Modal eliminar cliente
    clienteToDelete?.let { cliente ->
        AlertDialog(
            onDismissRequest = { clienteToDelete = null },
            title = { Text("¿Eliminar cliente?") },
            text = { Text("Se eliminará a '${cliente.nombre}' y su historial. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarCliente(cliente)
                        clienteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ClienteCardItem(
    item: ClienteConSaldo,
    moneda: String,
    onClick: () -> Unit,
    onCall: (String) -> Unit
) {
    val cli = item.cliente
    val tieneDeuda = item.saldoPendiente > 0.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("cliente_card_${cli.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (tieneDeuda) SoftRed.copy(alpha = 0.15f) else SoftGreen.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (tieneDeuda) "❗" else "✔",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                if (cli.direccion.isNotBlank()) {
                    Text(text = "📍 ${cli.direccion}", fontSize = 11.sp, color = SoftGray, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCurrency(item.saldoPendiente, moneda),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (tieneDeuda) SoftRed else SoftGreen
                )
                Text(
                    text = if (tieneDeuda) "Deuda Pendiente" else "Al Día",
                    fontSize = 10.sp,
                    color = if (tieneDeuda) SoftRed else SoftGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ClienteDetalleView(
    clienteConSaldo: ClienteConSaldo,
    transacciones: List<Transaccion>,
    moneda: String,
    nombreComercio: String,
    mensajeCobro: String,
    onBack: () -> Unit,
    onRegistrarTx: (String) -> Unit,
    onDeleteTx: (Transaccion) -> Unit,
    onDeleteCliente: () -> Unit
) {
    val context = LocalContext.current
    val cli = clienteConSaldo.cliente
    val deuda = clienteConSaldo.saldoPendiente

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header con botón regresar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cli.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (cli.telefono.isNotBlank()) {
                    Text("Tel: ${cli.telefono}", fontSize = 12.sp, color = SoftGray)
                }
            }
            IconButton(onClick = onDeleteCliente) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar cliente", tint = SoftRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Balance Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (deuda > 0) SoftRed.copy(alpha = 0.12f) else SoftGreen.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Saldo Actual Deudor", fontSize = 12.sp, color = if (deuda > 0) SoftRed else SoftGreen)
                        Text(
                            text = FormatUtils.formatCurrency(deuda, moneda),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (deuda > 0) SoftRed else SoftGreen
                        )
                    }

                    if (cli.telefono.isNotBlank() && deuda > 0) {
                        Button(
                            onClick = {
                                val textoMsg = mensajeCobro.replace("%MONTO%", FormatUtils.formatCurrency(deuda, moneda))
                                val cleanTel = cli.telefono.replace(Regex("[^0-9+]"), "").replace("+", "")
                                try {
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanTel&text=${Uri.encode(textoMsg)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Cobrar WhatsApp", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botones de acción rápida: + Venta Crédito / + Abono
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onRegistrarTx("COMPRA") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown)
            ) {
                Text("+ Venta a Crédito", fontSize = 12.sp)
            }
            Button(
                onClick = { onRegistrarTx("ABONO") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SoftGreen)
            ) {
                Text("+ Registrar Abono", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Historial de Cuenta y Movimientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (transacciones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay movimientos registrados para este cliente.", color = SoftGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transacciones) { tx ->
                    val isAbono = tx.tipo == "ABONO"
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAbono) "Abono / Pago" else "Compra a Crédito",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isAbono) SoftGreen else SoftRed
                                )
                                Text(tx.detalle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(FormatUtils.formatDate(tx.fecha), fontSize = 10.sp, color = SoftGray)
                            }
                            Text(
                                text = (if (isAbono) "- " else "+ ") + FormatUtils.formatCurrency(tx.montoTotal, moneda),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isAbono) SoftGreen else SoftRed
                            )
                            IconButton(onClick = { onDeleteTx(tx) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Eliminar", tint = SoftGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualTxDialog(
    tipo: String,
    clienteNombre: String,
    moneda: String,
    onDismiss: () -> Unit,
    onConfirm: (monto: Double, detalle: String) -> Unit
) {
    var montoStr by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf(if (tipo == "ABONO") "Abono en efectivo / transferencia" else "Consumo a crédito") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tipo == "ABONO") "Registrar Abono ($moneda)" else "Venta a Crédito ($moneda)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cliente: $clienteNombre", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it },
                    label = { Text("Detalle o Nota") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoStr.toDoubleOrNull() ?: 0.0
                    if (monto > 0) onConfirm(monto, detalle)
                },
                enabled = (montoStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun NuevoClienteDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, telefono: String, direccion: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Completo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank()) onConfirm(nombre, telefono, direccion) },
                enabled = nombre.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
