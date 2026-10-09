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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.data.model.Proveedor
import com.example.data.repository.ProveedorConSaldo
import com.example.ui.SazonViewModel
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
                shape = RoundedCornerShape(16.dp),
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
                        Text("Catálogo de Proveedores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${proveedores.size} registrados", fontSize = 12.sp, color = SoftGray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total CXP a Proveedores", fontSize = 11.sp, color = SoftRed)
                        Text(
                            text = FormatUtils.formatCurrency(totalCxp, config.monedaSimbolo),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRed
                        )
                        Text(
                            text = FormatUtils.formatBs(totalCxp, config.tasaCambioBs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    .testTag("search_proveedor_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

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
