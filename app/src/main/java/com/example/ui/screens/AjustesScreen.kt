package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfiguracionComercio
import com.example.data.model.Plato
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun AjustesScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.configuracion.collectAsState()
    val platos by viewModel.platos.collectAsState()

    var nombreComercio by remember(config) { mutableStateOf(config.nombreComercio) }
    var monedaSimbolo by remember(config) { mutableStateOf(config.monedaSimbolo) }
    var telefono by remember(config) { mutableStateOf(config.telefono) }
    var direccion by remember(config) { mutableStateOf(config.direccion) }
    var mensajeCobro by remember(config) { mutableStateOf(config.mensajeCobro) }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    var showAddPlatoDialog by remember { mutableStateOf(false) }
    var platoToEdit by remember { mutableStateOf<Plato?>(null) }
    var platoToDelete by remember { mutableStateOf<Plato?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section: Comercio Settings
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Datos del Comercio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Configura el nombre y símbolo de moneda de tu negocio.",
                        fontSize = 12.sp,
                        color = SoftGray
                    )

                    OutlinedTextField(
                        value = nombreComercio,
                        onValueChange = { nombreComercio = it },
                        label = { Text("Nombre del Comercio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ajustes_nombre_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = monedaSimbolo,
                            onValueChange = { monedaSimbolo = it },
                            label = { Text("Símbolo Moneda") },
                            placeholder = { Text("$, Bs, COP...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ajustes_moneda_input")
                        )
                        OutlinedTextField(
                            value = telefono,
                            onValueChange = { telefono = it },
                            label = { Text("Teléfono Local") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección / Ubicación") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mensajeCobro,
                        onValueChange = { mensajeCobro = it },
                        label = { Text("Mensaje Predeterminado para WhatsApp") },
                        supportingText = { Text("Usa %MONTO% para insertar la deuda automáticamente") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.guardarConfiguracion(
                                ConfiguracionComercio(
                                    nombreComercio = nombreComercio.trim().ifBlank { "Angel's Cafe" },
                                    monedaSimbolo = monedaSimbolo.trim().ifBlank { "$" },
                                    telefono = telefono.trim(),
                                    direccion = direccion.trim(),
                                    mensajeCobro = mensajeCobro.trim()
                                )
                            )
                            showSavedSnackbar = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }

                    if (showSavedSnackbar) {
                        Text("¡Configuración guardada exitosamente! ✔", color = SoftGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Menu & Products Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Catálogo de Platos y Menú", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${platos.size} platos registrados para la venta", fontSize = 12.sp, color = SoftGray)
                }
                Button(
                    onClick = { showAddPlatoDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CafeBrown),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Plato", fontSize = 12.sp)
                }
            }
        }

        items(platos) { plato ->
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
                        Text(plato.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (plato.descripcion.isNotBlank()) {
                            Text(plato.descripcion, fontSize = 12.sp, color = SoftGray, maxLines = 1)
                        }
                        Text(
                            FormatUtils.formatCurrency(plato.precio, config.monedaSimbolo),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CafeBrown
                        )
                    }
                    IconButton(onClick = { platoToEdit = plato }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CafeBrown, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { platoToDelete = plato }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SoftRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Section: App Information & GitHub Actions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Acerca del Sistema", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistema de Gestión Comercial para Cafés, Restaurantes y Comercios con cuentas por cobrar (CXC), cuentas por pagar (CXP) y punto de venta.",
                        fontSize = 12.sp,
                        color = SoftGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Versión: 2.0 (Menú Lateral Completo)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("• Base de Datos: Room SQLite Offline Local", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }

    if (showAddPlatoDialog) {
        PlatoDialog(
            plato = null,
            moneda = config.monedaSimbolo,
            onDismiss = { showAddPlatoDialog = false },
            onConfirm = { nombre, precio, desc, cat ->
                viewModel.agregarPlato(nombre, precio, desc, cat)
                showAddPlatoDialog = false
            }
        )
    }

    platoToEdit?.let { plato ->
        PlatoDialog(
            plato = plato,
            moneda = config.monedaSimbolo,
            onDismiss = { platoToEdit = null },
            onConfirm = { nombre, precio, desc, cat ->
                viewModel.editarPlato(plato.copy(nombre = nombre, precio = precio, descripcion = desc, categoria = cat))
                platoToEdit = null
            }
        )
    }

    platoToDelete?.let { plato ->
        AlertDialog(
            onDismissRequest = { platoToDelete = null },
            title = { Text("¿Eliminar plato?") },
            text = { Text("Se eliminará '${plato.nombre}' del menú.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarPlato(plato)
                        platoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { platoToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PlatoDialog(
    plato: Plato?,
    moneda: String,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, precio: Double, desc: String, cat: String) -> Unit
) {
    var nombre by remember { mutableStateOf(plato?.nombre ?: "") }
    var precioStr by remember { mutableStateOf(plato?.precio?.toString() ?: "") }
    var descripcion by remember { mutableStateOf(plato?.descripcion ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plato == null) "Nuevo Plato / Bebida" else "Editar Plato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Plato *") },
                    placeholder = { Text("Ej. Café Moca, Sandwich...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precioStr,
                    onValueChange = { precioStr = it },
                    label = { Text("Precio de Venta ($moneda) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción o Ingredientes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = precioStr.toDoubleOrNull() ?: 0.0
                    if (nombre.isNotBlank() && p > 0) onConfirm(nombre, p, descripcion, "General")
                },
                enabled = nombre.isNotBlank() && (precioStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
