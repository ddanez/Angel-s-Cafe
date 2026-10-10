package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.data.model.Compra
import com.example.data.model.Proveedor
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FiltroPeriodo
import com.example.ui.util.FormatUtils
import com.example.ui.util.SelectorPeriodoBar
import com.example.ui.util.TipoPeriodo

@Composable
fun ComprasScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.configuracion.collectAsState()
    val compras by viewModel.compras.collectAsState()
    val proveedores by viewModel.proveedoresConSaldo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var compraToDelete by remember { mutableStateOf<Compra?>(null) }

    // Selector de Período temporal (Día, Semana, Mes, Año, Específico, Todo)
    var filtroPeriodo by remember { mutableStateOf(FiltroPeriodo.porDefecto(TipoPeriodo.MES)) }

    val comprasPeriodo = remember(compras, filtroPeriodo) {
        compras.filter { filtroPeriodo.coincide(it.fecha) }
    }

    val totalCompras = remember(comprasPeriodo) { comprasPeriodo.sumOf { it.montoTotal } }
    val totalContado = remember(comprasPeriodo) { comprasPeriodo.filter { it.condicion == "CONTADO" }.sumOf { it.montoTotal } }
    val totalCredito = remember(comprasPeriodo) { comprasPeriodo.filter { it.condicion == "CREDITO" }.sumOf { it.montoTotal } }

    val filteredCompras = remember(searchQuery, comprasPeriodo) {
        if (searchQuery.isBlank()) comprasPeriodo
        else comprasPeriodo.filter {
            it.proveedorNombre.contains(searchQuery, ignoreCase = true) ||
            it.detalle.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_compra_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Compra")
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
            // Selector de Período
            SelectorPeriodoBar(
                filtro = filtroPeriodo,
                onFiltroCambiado = { filtroPeriodo = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Card
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
                        Text("Compras del Período", fontSize = 12.sp, color = SoftGray)
                        Text("${comprasPeriodo.size} registros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CafeBrown)
                    }
                    Text(
                        text = FormatUtils.formatCurrency(totalCompras, config.monedaSimbolo),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Equivalente: ${FormatUtils.formatBs(totalCompras, config.tasaCambioBs)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CafeDarkBrown
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Contado: ${FormatUtils.formatDual(totalContado, config.tasaCambioBs, config.monedaSimbolo)}", fontSize = 11.sp, color = SoftGreen)
                        Text("Crédito (CXP): ${FormatUtils.formatDual(totalCredito, config.tasaCambioBs, config.monedaSimbolo)}", fontSize = 11.sp, color = SoftRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar compras o proveedor...", color = SoftGray) },
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

            if (filteredCompras.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No hay compras registradas." else "No hay resultados.",
                            color = SoftGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCompras) { c ->
                        val esCredito = c.condicion == "CREDITO"
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = c.proveedorNombre.ifBlank { "Compra General" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    if (esCredito) "Crédito (CXP)" else "Contado",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (esCredito) SoftRed else SoftGreen
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (esCredito) SoftRed.copy(alpha = 0.12f) else SoftGreen.copy(alpha = 0.12f)
                                            ),
                                            border = null,
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                    Text(
                                        text = c.detalle,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = FormatUtils.formatDate(c.fecha),
                                        fontSize = 10.sp,
                                        color = SoftGray
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FormatUtils.formatCurrency(c.montoTotal, config.monedaSimbolo),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = FormatUtils.formatBs(c.montoTotal, config.tasaCambioBs),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = CafeDarkBrown
                                    )
                                    IconButton(
                                        onClick = { compraToDelete = c },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SoftRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RegistrarCompraDialog(
            listaProveedores = proveedores.map { it.proveedor },
            moneda = config.monedaSimbolo,
            onDismiss = { showAddDialog = false },
            onConfirm = { provId, provNombre, detalle, monto, condicion ->
                viewModel.registrarCompra(provId, provNombre, detalle, monto, condicion)
                showAddDialog = false
            }
        )
    }

    compraToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { compraToDelete = null },
            title = { Text("¿Eliminar compra?") },
            text = { Text("Se eliminará el registro de '${c.detalle}'.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarCompra(c)
                        compraToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { compraToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrarCompraDialog(
    listaProveedores: List<Proveedor>,
    moneda: String,
    onDismiss: () -> Unit,
    onConfirm: (provId: Int, provNombre: String, detalle: String, monto: Double, condicion: String) -> Unit
) {
    var selectedProv by remember { mutableStateOf<Proveedor?>(listaProveedores.firstOrNull()) }
    var customProvName by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var montoStr by remember { mutableStateOf("") }
    var condicion by remember { mutableStateOf("CONTADO") } // CONTADO o CREDITO
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Compra / Insumos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Proveedor selector
                if (listaProveedores.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedProv?.nombre ?: "Sin proveedor seleccionado",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Proveedor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            listaProveedores.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.nombre} (${p.empresa})") },
                                    onClick = {
                                        selectedProv = p
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customProvName,
                        onValueChange = { customProvName = it },
                        label = { Text("Nombre del Proveedor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it },
                    label = { Text("Descripción / Insumos comprados *") },
                    placeholder = { Text("Ej. Café grano 5kg, Leche, Queso...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto Total ($moneda) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Condición de pago
                Text("Condición de Pago:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = condicion == "CONTADO",
                        onClick = { condicion = "CONTADO" },
                        label = { Text("Contado 💵") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = condicion == "CREDITO",
                        onClick = { condicion = "CREDITO" },
                        label = { Text("Crédito (CXP) 📑") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoStr.toDoubleOrNull() ?: 0.0
                    val provId = selectedProv?.id ?: 0
                    val provNombre = selectedProv?.nombre ?: customProvName.ifBlank { "Proveedor General" }
                    if (monto > 0 && detalle.isNotBlank()) {
                        onConfirm(provId, provNombre, detalle.trim(), monto, condicion)
                    }
                },
                enabled = (montoStr.toDoubleOrNull() ?: 0.0) > 0.0 && detalle.isNotBlank()
            ) {
                Text("Guardar Compra")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
