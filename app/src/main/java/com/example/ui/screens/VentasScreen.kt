package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Cliente
import com.example.data.model.Plato
import com.example.data.model.Transaccion
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FiltroPeriodo
import com.example.ui.util.FormatUtils
import com.example.ui.util.SelectorPeriodoBar
import com.example.ui.util.TipoPeriodo

@Composable
fun VentasScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val platos by viewModel.platos.collectAsState()
    val carrito by viewModel.carrito.collectAsState()
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val transacciones by viewModel.transacciones.collectAsState()

    var tabSeleccionada by remember { mutableStateOf(0) } // 0: Punto de Venta / Menú, 1: Historial de Ventas
    var searchQuery by remember { mutableStateOf("") }
    var searchQueryHistorial by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var ticketGenerado by remember { mutableStateOf<String?>(null) }

    // Selector de período temporal para el historial de ventas
    var filtroPeriodo by remember { mutableStateOf(FiltroPeriodo.porDefecto(TipoPeriodo.DIA)) }

    val totalCarrito = remember(carrito) {
        carrito.entries.sumOf { it.key.precio * it.value }
    }
    val totalItems = remember(carrito) {
        carrito.values.sum()
    }

    val filteredPlatos = remember(searchQuery, platos) {
        if (searchQuery.isBlank()) platos
        else platos.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) ||
            it.descripcion.contains(searchQuery, ignoreCase = true)
        }
    }

    // Ventas filtradas por período (Día, Semana, Mes, Año, Específico, Todo)
    val ventasDelPeriodo = remember(transacciones, filtroPeriodo, searchQueryHistorial) {
        transacciones
            .filter { (it.tipo == "VENTA_CONTADO" || it.tipo == "COMPRA") && filtroPeriodo.coincide(it.fecha) }
            .filter {
                if (searchQueryHistorial.isBlank()) true
                else it.detalle.contains(searchQueryHistorial, ignoreCase = true)
            }
            .sortedByDescending { it.fecha }
    }

    val totalVentasPeriodo = remember(ventasDelPeriodo) {
        ventasDelPeriodo.sumOf { it.montoTotal }
    }
    val totalVentasContadoPeriodo = remember(ventasDelPeriodo) {
        ventasDelPeriodo.filter { it.tipo == "VENTA_CONTADO" }.sumOf { it.montoTotal }
    }
    val totalVentasCreditoPeriodo = remember(ventasDelPeriodo) {
        ventasDelPeriodo.filter { it.tipo == "COMPRA" }.sumOf { it.montoTotal }
    }

    Scaffold(
        bottomBar = {
            if (tabSeleccionada == 0 && totalItems > 0) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$totalItems producto(s) en orden",
                                fontSize = 12.sp,
                                color = SoftGray
                            )
                            Text(
                                text = FormatUtils.formatDual(totalCarrito, config.tasaCambioBs, config.monedaSimbolo),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.limpiarCarrito() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed)
                            ) {
                                Text("Vaciar", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showCheckoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenCrema, contentColor = DarkText),
                                modifier = Modifier.testTag("cobrar_carrito_btn")
                            ) {
                                Text("Cobrar 🛍️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gestión de Ventas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Punto de Venta y Registro de Comandas", fontSize = 12.sp, color = SoftGray)
                }
                Text("☕ ${config.nombreComercio}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CafeBrown)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs de Navegación de Ventas
            TabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0 },
                    text = { Text("🛍️ Punto de Venta", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1 },
                    text = { Text("🧾 Historial (${ventasDelPeriodo.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tabSeleccionada == 0) {
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar café, empanada, desayuno...", color = SoftGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CafeBrown) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Menu Catalog Grid / List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = if (totalItems > 0) 80.dp else 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPlatos) { plato ->
                        val count = carrito[plato] ?: 0
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("plato_item_${plato.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plato.nombre,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (plato.descripcion.isNotBlank()) {
                                        Text(
                                            text = plato.descripcion,
                                            fontSize = 12.sp,
                                            color = SoftGray,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatDual(plato.precio, config.tasaCambioBs, config.monedaSimbolo),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CafeDarkBrown
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (count > 0) {
                                        IconButton(onClick = { viewModel.removerDelCarrito(plato) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = SoftRed)
                                        }
                                        Text(
                                            text = "$count",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.agregarAlCarrito(plato) }) {
                                        Icon(Icons.Default.Add, contentDescription = "Agregar", tint = SoftGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- PESTAÑA 1: HISTORIAL DE VENTAS POR PERÍODO ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Selector de Período temporal
                    item {
                        SelectorPeriodoBar(
                            filtro = filtroPeriodo,
                            onFiltroCambiado = { filtroPeriodo = it }
                        )
                    }

                    // Card de métricas de ventas en el período
                    item {
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
                                    Text("Facturación del Período", fontSize = 12.sp, color = SoftGray)
                                    Text("${ventasDelPeriodo.size} tickets", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CafeBrown)
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(totalVentasPeriodo, config.monedaSimbolo),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Equivalente: ${FormatUtils.formatBs(totalVentasPeriodo, config.tasaCambioBs)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CafeDarkBrown
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Contado: ${FormatUtils.formatDual(totalVentasContadoPeriodo, config.tasaCambioBs, config.monedaSimbolo)}", fontSize = 11.sp, color = SoftGreen)
                                    Text("Crédito (CXC): ${FormatUtils.formatDual(totalVentasCreditoPeriodo, config.tasaCambioBs, config.monedaSimbolo)}", fontSize = 11.sp, color = SoftRed)
                                }
                            }
                        }
                    }

                    // Buscador dentro del historial
                    item {
                        OutlinedTextField(
                            value = searchQueryHistorial,
                            onValueChange = { searchQueryHistorial = it },
                            placeholder = { Text("Buscar ticket o detalle...", color = SoftGray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CafeBrown) },
                            trailingIcon = {
                                if (searchQueryHistorial.isNotEmpty()) {
                                    IconButton(onClick = { searchQueryHistorial = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    if (ventasDelPeriodo.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🧾", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No hay ventas registradas en ${filtroPeriodo.textoDescriptivo}",
                                        fontSize = 13.sp,
                                        color = SoftGray
                                    )
                                }
                            }
                        }
                    } else {
                        items(ventasDelPeriodo) { venta ->
                            val esCredito = venta.tipo == "COMPRA"
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (esCredito) "📋" else "💵", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (esCredito) "Venta a Crédito (CXC)" else "Venta de Contado",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (esCredito) CafeBrown else SoftGreen
                                            )
                                        }
                                        Text(
                                            text = FormatUtils.formatDate(venta.fecha),
                                            fontSize = 11.sp,
                                            color = SoftGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = venta.detalle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = FormatUtils.formatCurrency(venta.montoTotal, config.monedaSimbolo),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = FormatUtils.formatBs(venta.montoTotal, config.tasaCambioBs),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CafeDarkBrown
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val ticketReimpreso = """
🧾 ${config.nombreComercio}
Comprobante de Venta #${venta.id}
----------------------------------
Fecha: ${FormatUtils.formatDate(venta.fecha)}
Condición: ${if (esCredito) "Crédito (CXC)" else "Contado"}
Detalle:
${venta.detalle}
----------------------------------
TOTAL USD: ${FormatUtils.formatCurrency(venta.montoTotal, config.monedaSimbolo)}
TOTAL BS: ${FormatUtils.formatBs(venta.montoTotal, config.tasaCambioBs)}
Tasa Oficial BCV: 1 USD = Bs. ${String.format(java.util.Locale.US, "%.2f", config.tasaCambioBs)}
¡Gracias por su preferencia! ☕
                                                """.trimIndent()
                                                ticketGenerado = ticketReimpreso
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ticket", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Checkout / Cobro
    if (showCheckoutDialog) {
        CheckoutVentaDialog(
            listaClientes = clientes.map { it.cliente },
            total = totalCarrito,
            tasaCambioBs = config.tasaCambioBs,
            moneda = config.monedaSimbolo,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { esCredito, clienteId, clienteNombre, nota ->
                val resumenItems = carrito.entries.joinToString("\n") {
                    val subtotal = it.key.precio * it.value
                    "• ${it.value}x ${it.key.nombre} - ${FormatUtils.formatDual(subtotal, config.tasaCambioBs, config.monedaSimbolo)}"
                }
                val ticket = """
🧾 ${config.nombreComercio}
Comprobante de Venta (Doble Moneda)
----------------------------------
Cliente: ${if (esCredito) clienteNombre else "Mostrador / Contado"}
Condición: ${if (esCredito) "Crédito (CXC)" else "Contado"}
${if (nota.isNotBlank()) "Mesa/Nota: $nota\n" else ""}Tasa del Día: 1 USD = Bs. ${String.format(java.util.Locale.US, "%.2f", config.tasaCambioBs)}
----------------------------------
Detalle:
$resumenItems
----------------------------------
TOTAL EN DÓLARES: ${FormatUtils.formatCurrency(totalCarrito, config.monedaSimbolo)}
TOTAL EN BOLÍVARES: ${FormatUtils.formatBs(totalCarrito, config.tasaCambioBs)}
¡Gracias por su compra! ☕
                """.trimIndent()

                viewModel.registrarVentaCarrito(clienteId, esCredito, nota)
                showCheckoutDialog = false
                ticketGenerado = ticket
            }
        )
    }

    // Modal de Ticket generado
    ticketGenerado?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketGenerado = null },
            title = { Text("Comprobante de Venta 🧾") },
            text = {
                Column {
                    Text(
                        text = ticket,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, ticket)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir Comprobante"))
                        ticketGenerado = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGreen)
                ) {
                    Text("Compartir Ticket / WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { ticketGenerado = null }) { Text("Cerrar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutVentaDialog(
    listaClientes: List<Cliente>,
    total: Double,
    tasaCambioBs: Double,
    moneda: String,
    onDismiss: () -> Unit,
    onConfirm: (esCredito: Boolean, clienteId: Int, clienteNombre: String, nota: String) -> Unit
) {
    var esCredito by remember { mutableStateOf(false) }
    var selectedCliente by remember { mutableStateOf<Cliente?>(listaClientes.firstOrNull()) }
    var notaMesa by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalizar Venta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total a Cobrar:",
                            fontSize = 11.sp,
                            color = SoftGray
                        )
                        Text(
                            text = FormatUtils.formatCurrency(total, moneda),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Equivalente en Bolívares: ${FormatUtils.formatBs(total, tasaCambioBs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CafeDarkBrown
                        )
                        Text(
                            text = "(Tasa: 1 USD = Bs. ${String.format(java.util.Locale.US, "%.2f", tasaCambioBs)})",
                            fontSize = 10.sp,
                            color = SoftGray
                        )
                    }
                }

                Text("Método de Facturación:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !esCredito,
                        onClick = { esCredito = false },
                        label = { Text("Contado 💵") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = esCredito,
                        onClick = { esCredito = true },
                        label = { Text("A Crédito 📋") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (esCredito) {
                    if (listaClientes.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedCliente?.nombre ?: "Seleccionar cliente",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cargar a cuenta de cliente:") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                listaClientes.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.nombre) },
                                        onClick = {
                                            selectedCliente = c
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No hay clientes registrados en el directorio. La venta se registrará como mostrador o agrega clientes primero.",
                            color = SoftRed,
                            fontSize = 11.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = notaMesa,
                    onValueChange = { notaMesa = it },
                    label = { Text("Mesa o Nota adicional (opcional)") },
                    placeholder = { Text("Ej. Mesa 3 / Para llevar") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cliId = if (esCredito) (selectedCliente?.id ?: 0) else 0
                    val cliNombre = if (esCredito) (selectedCliente?.nombre ?: "Cliente General") else "Mostrador"
                    onConfirm(esCredito, cliId, cliNombre, notaMesa)
                }
            ) {
                Text("Confirmar Venta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
