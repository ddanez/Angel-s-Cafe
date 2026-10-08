package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Cliente
import com.example.data.model.Plato
import com.example.data.model.Transaccion
import com.example.data.repository.ClienteConSaldo
import com.example.ui.SazonViewModel
import com.example.ui.theme.CafeBrown
import com.example.ui.theme.CafeDarkBrown
import com.example.ui.theme.GoldenCrema
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftRed
import com.example.ui.theme.SoftGray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val platos by viewModel.platos.collectAsState()
    val carrito by viewModel.carrito.collectAsState()
    val selectedClienteId by viewModel.selectedClienteId.collectAsState()
    val transaccionesCliente by viewModel.transaccionesDelClienteSeleccionado.collectAsState()
    val todasTransacciones by viewModel.transacciones.collectAsState()

    // UI state
    var searchQueries by remember { mutableStateOf("") }
    var showAddClienteDialog by remember { mutableStateOf(false) }
    var showAddPlatoDialog by remember { mutableStateOf(false) }
    var showManualTxDialog by remember { mutableStateOf<String?>(null) } // "COMPRA" or "ABONO" or null

    val selectedCliente = remember(selectedClienteId, clientes) {
        clientes.find { it.cliente.id == selectedClienteId }
    }

    // Calculations for the financial card
    val totalCalculadoCxC = remember(clientes) {
        clientes.sumOf { it.saldoPendiente }
    }
    val deudoresActivos = remember(clientes) {
        clientes.count { it.saldoPendiente > 0.0 }
    }
    val recaudadoHoy = remember(todasTransacciones) {
        val hoy = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        todasTransacciones
            .filter { it.tipo == "ABONO" }
            .filter {
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.fecha)) == hoy
            }
            .sumOf { it.montoTotal }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Angel's Cafe 🍳",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    if (selectedCliente != null) {
                        IconButton(
                            onClick = { viewModel.selectCliente(null) },
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCliente == null) {
                FloatingActionButton(
                    onClick = { showAddClienteDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_cliente_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Cliente")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Summary Dashboard Card
            ResumenFinancieroCard(
                totalCxC = totalCalculadoCxC,
                recaudadoHoy = recaudadoHoy,
                deudoresActivos = deudoresActivos
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedCliente == null) {
                // Client Search Bar & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQueries,
                        onValueChange = { searchQueries = it },
                        placeholder = { Text("Buscar cliente...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CafeBrown) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("client_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of filtered clients
                val filteredClientes = remember(searchQueries, clientes) {
                    if (searchQueries.isBlank()) {
                        clientes
                    } else {
                        clientes.filter {
                            it.cliente.nombre.contains(searchQueries, ignoreCase = true) ||
                                    it.cliente.telefono.contains(searchQueries)
                        }
                    }
                }

                if (filteredClientes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = CafeBrown,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ningún cliente guardado.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "¡Crea uno abajo para llevar su cuenta!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredClientes) { cc ->
                            ClienteCardItem(
                                clienteConSaldo = cc,
                                onSelect = { viewModel.selectCliente(cc.cliente.id) },
                                onDelete = { viewModel.borrarCliente(cc.cliente) }
                            )
                        }
                    }
                }
            } else {
                // DETAILED CLIENT VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Selected Client Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedCliente.cliente.nombre,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (selectedCliente.cliente.telefono.isNotBlank()) {
                                        Text(
                                            text = "📞 ${selectedCliente.cliente.telefono}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Saldo actual",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = formatMonto(selectedCliente.saldoPendiente),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (selectedCliente.saldoPendiente > 0.0) SoftRed else SoftGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // WhatsApp Action Button
                                Button(
                                    onClick = {
                                        val mDetalles = transaccionesCliente
                                            .filter { it.tipo == "COMPRA" }
                                            .take(5)
                                            .joinToString("\n") { "- ${formatFecha(it.fecha)}: ${it.detalle} (${formatMonto(it.montoTotal)})" }

                                        val saludo = "Hola, *${selectedCliente.cliente.nombre}* 👋. Te enviamos el recordatorio de tu cuenta por cobrar en *Angel's Cafe*."
                                        val saldoTxt = "\n\nSaldo actual pendiente: *${formatMonto(selectedCliente.saldoPendiente)}*"
                                        val detalleTxt = if (mDetalles.isNotBlank()) "\n\nDetalle de últimas compras:\n$mDetalles" else ""
                                        val agradecimiento = "\n\n¡Muchas gracias por tu preferencia! 🍳 Te esperamos pronto."
                                        val msgVal = "$saludo$saldoTxt$detalleTxt$agradecimiento"

                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, msgVal)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Enviar recordatorio"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cobrar", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { showManualTxDialog = "ABONO" },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Abonar", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { showManualTxDialog = "COMPRA" },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compra", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Menu / Coffee list (Shopping experience)
                    Text(
                        text = "☕ Facturar del Menú",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Menu & Cart integration
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        if (platos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Cargando menú...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(platos) { plato ->
                                    val countInCart = carrito[plato] ?: 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.agregarAlCarrito(plato) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(plato.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                            Text(formatMonto(plato.precio), fontSize = 13.sp, color = CafeBrown)
                                        }
                                        if (countInCart > 0) {
                                            IconButton(onClick = { viewModel.removerDelCarrito(plato) }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Menos", tint = SoftRed)
                                            }
                                            Text("$countInCart", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                        IconButton(onClick = { viewModel.agregarAlCarrito(plato) }) {
                                            Icon(Icons.Default.Add, contentDescription = "Más", tint = SoftGreen)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }

                    // Cart Check out Button
                    if (carrito.isNotEmpty()) {
                        val cartTotal = carrito.entries.sumOf { it.key.precio * it.value }
                        Button(
                            onClick = { viewModel.registrarVentaCarrito(selectedCliente.cliente.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenCrema, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Facturar Carrito (${formatMonto(cartTotal)}) 🛒",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ledger list title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historial de Cuenta",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showAddPlatoDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurar Platos", tint = CafeBrown)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // List of account logs/transacciones
                    if (transaccionesCliente.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay movimientos registrados en esta cuenta.", color = SoftGray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(transaccionesCliente) { tx ->
                                TransaccionItem(
                                    tx = tx,
                                    onDelete = { viewModel.borrarTransaccion(tx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // dialog -- ADD CLIENT
    if (showAddClienteDialog) {
        var nombre by remember { mutableStateOf("") }
        var telefono by remember { mutableStateOf("") }
        var direccion by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddClienteDialog = false },
            title = { Text("Registrar Nuevo Cliente ☕") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Teléfono / WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombre.isNotBlank()) {
                            viewModel.agregarCliente(nombre, telefono, direccion)
                            showAddClienteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClienteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // dialog -- CONFIGURE PLATO
    if (showAddPlatoDialog) {
        var fNombre by remember { mutableStateOf("") }
        var fPrecio by remember { mutableStateOf("") }
        var fDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPlatoDialog = false },
            title = { Text("Añadir al Menú del Café 🍰") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fNombre,
                        onValueChange = { fNombre = it },
                        label = { Text("Nombre del Plato/Bebida") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fPrecio,
                        onValueChange = { fPrecio = it },
                        label = { Text("Precio (0.00)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fDesc,
                        onValueChange = { fDesc = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fNombre.isNotBlank() && fPrecio.isNotBlank()) {
                            val prVal = fPrecio.toDoubleOrNull() ?: 0.0
                            viewModel.agregarPlato(fNombre, prVal, fDesc)
                            showAddPlatoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown)
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlatoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // dialog -- MANUAL TX (COMPRA or ABONO)
    showManualTxDialog?.let { tipo ->
        var tMonto by remember { mutableStateOf("") }
        var tDetalle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualTxDialog = null },
            title = { Text(if (tipo == "COMPRA") "Cargar Compra Directa 📝" else "Registrar Abono a Cuenta 💰") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tMonto,
                        onValueChange = { tMonto = it },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tDetalle,
                        onValueChange = { tDetalle = it },
                        placeholder = { Text(if (tipo == "COMPRA") "Ejem: 2 Empanadas, Almuerzo" else "Ejem: Abono fin de semana") },
                        label = { Text("Detalle / Observaciones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mVal = tMonto.toDoubleOrNull() ?: 0.0
                        if (mVal > 0.0 && selectedCliente != null) {
                            val finalDetalle = tDetalle.ifBlank { if (tipo == "COMPRA") "Compra especial" else "Abono general" }
                            viewModel.registrarVentaO_AbonoManual(
                                clienteId = selectedCliente.cliente.id,
                                monto = mVal,
                                detalle = finalDetalle,
                                tipo = tipo
                            )
                            showManualTxDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (tipo == "COMPRA") SoftRed else SoftGreen)
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTxDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// THE RESUMEN FINANCIERO CARD
@Composable
fun ResumenFinancieroCard(
    totalCxC: Double,
    recaudadoHoy: Double,
    deudoresActivos: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, CafeBrown.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Total accounts receivable
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Por Cobrar Total (CxC)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatMonto(totalCxC),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$deudoresActivos clientes activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // RIGHT: Abonos collected today
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Recaudado Hoy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatMonto(recaudadoHoy),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldenCrema
                    )
                    Text(
                        text = "Caja + abonos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ITEM COMPOSABLE FOR CLIENT CARD LISTING
@Composable
fun ClienteCardItem(
    clienteConSaldo: ClienteConSaldo,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("cliente_item_${clienteConSaldo.cliente.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clienteConSaldo.cliente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (clienteConSaldo.cliente.telefono.isNotBlank()) {
                    Text(
                        text = clienteConSaldo.cliente.telefono,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Saldo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatMonto(clienteConSaldo.saldoPendiente),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (clienteConSaldo.saldoPendiente > 0.0) SoftRed else SoftGreen
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar Cliente",
                        tint = SoftRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// TRANSACTION ITEM LOGS
@Composable
fun TransaccionItem(
    tx: Transaccion,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAbono = tx.tipo == "ABONO"
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored status indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isAbono) SoftGreen else SoftRed, RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tx.detalle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatFecha(tx.fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (isAbono) "-" else "+") + formatMonto(tx.montoTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isAbono) SoftGreen else SoftRed
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar transacción",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// HELPER formatting utilities
fun formatMonto(monto: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(monto)
}

fun formatFecha(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
