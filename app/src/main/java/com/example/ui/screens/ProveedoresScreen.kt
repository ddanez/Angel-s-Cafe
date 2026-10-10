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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Proveedor
import com.example.data.repository.ProveedorConSaldo
import com.example.ui.SazonViewModel
import com.example.ui.components.EditarTasaDialog
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun ProveedoresScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val proveedores by viewModel.proveedoresConSaldo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditarTasaDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Proveedor?>(null) }
    var supplierToDelete by remember { mutableStateOf<Proveedor?>(null) }

    val filteredList = remember(searchQuery, proveedores) {
        if (searchQuery.isBlank()) proveedores
        else proveedores.filter {
            it.proveedor.nombre.contains(searchQuery, ignoreCase = true) ||
            it.proveedor.empresa.contains(searchQuery, ignoreCase = true) ||
            it.proveedor.telefono.contains(searchQuery)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_proveedor_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Proveedor")
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
            // Summary header
            val totalCxp = remember(proveedores) { proveedores.sumOf { it.saldoPendienteCXP } }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Catálogo de Proveedores",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${proveedores.size} registrados",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total CXP:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoftRed
                        )
                        Text(
                            text = FormatUtils.formatCurrency(totalCxp, config.monedaSimbolo),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SoftRed
                        )
                        Text(
                            text = "(${FormatUtils.formatBs(totalCxp, config.tasaCambioBs)})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftRed
                        )
                        Surface(
                            onClick = { showEditarTasaDialog = true },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Tasa",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ajustar Tasa Manual",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search (optimizado para mínima altura)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CafeBrown,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar proveedor o empresa...",
                                color = SoftGray,
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_proveedor_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = SoftGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚚", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No hay proveedores registrados aún." else "No se encontraron coincidencias.",
                            color = SoftGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        ProveedorItemCard(
                            item = item,
                            moneda = config.monedaSimbolo,
                            tasaCambioBs = config.tasaCambioBs,
                            onEdit = { supplierToEdit = item.proveedor },
                            onDelete = { supplierToDelete = item.proveedor },
                            onCall = { tel ->
                                if (tel.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                                    context.startActivity(intent)
                                }
                            },
                            onWhatsApp = { tel, nombre ->
                                if (tel.isNotBlank()) {
                                    openWhatsApp(context, tel, "Hola $nombre, le escribimos de ${config.nombreComercio}.")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProveedorDialog(
            proveedor = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { nombre, empresa, tel, dir, notas ->
                viewModel.agregarProveedor(nombre, empresa, tel, dir, notas)
                showAddDialog = false
            }
        )
    }

    supplierToEdit?.let { prov ->
        ProveedorDialog(
            proveedor = prov,
            onDismiss = { supplierToEdit = null },
            onConfirm = { nombre, empresa, tel, dir, notas ->
                viewModel.editarProveedor(prov.copy(nombre = nombre, empresa = empresa, telefono = tel, direccion = dir, notas = notas))
                supplierToEdit = null
            }
        )
    }

    supplierToDelete?.let { prov ->
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = { Text("¿Eliminar proveedor?") },
            text = { Text("Se eliminará '${prov.nombre}'. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarProveedor(prov)
                        supplierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditarTasaDialog) {
        EditarTasaDialog(
            tasaActual = config.tasaCambioBs,
            onDismiss = { showEditarTasaDialog = false },
            onConfirm = { nuevaTasa ->
                viewModel.actualizarTasaCambioManual(nuevaTasa)
                showEditarTasaDialog = false
            }
        )
    }
}

@Composable
private fun ProveedorItemCard(
    item: ProveedorConSaldo,
    moneda: String,
    tasaCambioBs: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String, String) -> Unit
) {
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (prov.empresa.isNotBlank()) {
                        Text(
                            text = prov.empresa,
                            fontSize = 12.sp,
                            color = CafeBrown,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (item.saldoPendienteCXP > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Deuda (CXP)", fontSize = 10.sp, color = SoftRed)
                        Text(
                            text = FormatUtils.formatCurrency(item.saldoPendienteCXP, moneda),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SoftRed
                        )
                        Text(
                            text = FormatUtils.formatBs(item.saldoPendienteCXP, tasaCambioBs),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = SoftRed
                        )
                    }
                }
            }

            if (prov.telefono.isNotBlank() || prov.direccion.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                if (prov.telefono.isNotBlank()) {
                    Text("📞 ${prov.telefono}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
                if (prov.direccion.isNotBlank()) {
                    Text("📍 ${prov.direccion}", fontSize = 11.sp, color = SoftGray)
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
                    if (prov.telefono.isNotBlank()) {
                        FilledTonalButton(
                            onClick = { onWhatsApp(prov.telefono, prov.nombre) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SoftGreen.copy(alpha = 0.15f), contentColor = SoftGreen)
                        ) {
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onCall(prov.telefono) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = CafeBrown)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CafeBrown)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SoftRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProveedorDialog(
    proveedor: Proveedor?,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, empresa: String, tel: String, dir: String, notas: String) -> Unit
) {
    var nombre by remember { mutableStateOf(proveedor?.nombre ?: "") }
    var empresa by remember { mutableStateOf(proveedor?.empresa ?: "") }
    var telefono by remember { mutableStateOf(proveedor?.telefono ?: "") }
    var direccion by remember { mutableStateOf(proveedor?.direccion ?: "") }
    var notas by remember { mutableStateOf(proveedor?.notas ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (proveedor == null) "Nuevo Proveedor" else "Editar Proveedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del contacto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = empresa,
                    onValueChange = { empresa = it },
                    label = { Text("Empresa o Razón Social") },
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
                    label = { Text("Dirección o Ubicación") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Insumos o Notas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank()) onConfirm(nombre, empresa, telefono, direccion, notas) },
                enabled = nombre.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun openWhatsApp(context: Context, phone: String, message: String) {
    val clean = phone.replace(Regex("[^0-9+]"), "").replace("+", "")
    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    }
}
