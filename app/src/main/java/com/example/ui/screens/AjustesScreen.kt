package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfiguracionComercio
import com.example.data.model.Plato
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AjustesScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
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

    // Dialogos para Licenciamiento y Hard Reset
    var showLicenciaDialog by remember { mutableStateOf(false) }
    var showHardResetDialog by remember { mutableStateOf(false) }
    var showHardResetSuccessDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
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
                                config.copy(
                                    nombreComercio = nombreComercio.trim().ifBlank { "Angel's Cafe" },
                                    monedaSimbolo = monedaSimbolo.trim().ifBlank { "$" },
                                    telefono = telefono.trim(),
                                    direccion = direccion.trim(),
                                    mensajeCobro = mensajeCobro.trim()
                                )
                            )
                            showSavedSnackbar = true
                            Toast.makeText(context, "Datos del comercio actualizados", Toast.LENGTH_SHORT).show()
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

        // Section: LICENCIAMIENTO DEL SISTEMA
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("card_licenciamiento")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Licenciamiento del Software",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = if (config.estadoLicencia == "ACTIVA") SoftGreen.copy(alpha = 0.18f) else SoftRed.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (config.estadoLicencia == "ACTIVA") "✓ ACTIVA" else "NO REGISTRADA",
                                color = if (config.estadoLicencia == "ACTIVA") SoftGreen else SoftRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Estado y registro de la licencia comercial del sistema para este terminal de punto de venta.",
                        fontSize = 12.sp,
                        color = SoftGray
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Titular:", fontSize = 12.sp, color = SoftGray)
                                Text(config.titularLicencia, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tipo:", fontSize = 12.sp, color = SoftGray)
                                Text(config.tipoLicencia, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CafeDarkBrown)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Clave / Serial:", fontSize = 12.sp, color = SoftGray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        config.claveLicencia,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CafeBrown
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(config.claveLicencia))
                                            Toast.makeText(context, "Clave copiada al portapapeles", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Copiar", tint = CafeBrown, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showLicenciaDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CafeDarkBrown),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CafeDarkBrown)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Activar / Modificar Licencia", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: HARD RESET (REINICIO TOTAL DEL SISTEMA)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SoftRed.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("card_hard_reset")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Zona de Peligro: Hard Reset",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SoftRed
                            )
                            Text(
                                text = "Restablecimiento de Fábrica de la Base de Datos",
                                fontSize = 11.sp,
                                color = SoftRed.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Text(
                        text = "El Hard Reset vacía completamente todas las tablas del sistema: ventas, clientes, compras, inventario, materias primas, deudas (CXC/CXP) y recetas, dejando la aplicación como nueva.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { showHardResetDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("button_open_hard_reset")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ejecutar Hard Reset (Restaurar de Fábrica)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: App Information & Specs
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
                        text = "Sistema de Gestión Comercial para Cafés, Restaurantes y Comercios con cuentas por cobrar (CXC), cuentas por pagar (CXP), inventario doble (materia prima y producto terminado con receta) y punto de venta.",
                        fontSize = 12.sp,
                        color = SoftGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Versión: 2.1 (Módulos Completos + Doble Inventario + Licencia + Hard Reset)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("• Base de Datos: Room SQLite Offline Local", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("• Licencia: ${config.tipoLicencia}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }

    // --- DIÁLOGOS DE PLATOS ---
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

    // --- DIÁLOGO DE LICENCIAMIENTO ---
    if (showLicenciaDialog) {
        LicenciaDialog(
            configActual = config,
            onDismiss = { showLicenciaDialog = false },
            onActivar = { clave, titular ->
                viewModel.activarLicencia(clave, titular) { exito, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (exito) showLicenciaDialog = false
                }
            }
        )
    }

    // --- DIÁLOGO DE HARD RESET (CONFIRMACIÓN DE SEGURIDAD) ---
    if (showHardResetDialog) {
        HardResetConfirmDialog(
            onDismiss = { showHardResetDialog = false },
            onConfirm = { conDatosEjemplo ->
                showHardResetDialog = false
                viewModel.ejecutarHardReset(conDatosEjemplo) {
                    showHardResetSuccessDialog = true
                }
            }
        )
    }

    // --- DIÁLOGO DE ÉXITO TRAS HARD RESET ---
    if (showHardResetSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showHardResetSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(36.dp)) },
            title = { Text("¡Hard Reset Completado!", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "La base de datos ha sido restablecida con éxito. Todas las tablas quedaron limpias y preparadas para operar nuevamente con total rapidez.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { showHardResetSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown)
                ) {
                    Text("Aceptar y Continuar")
                }
            }
        )
    }
}

// DIALOG: ACTIVACIÓN / GESTIÓN DE LICENCIA
@Composable
private fun LicenciaDialog(
    configActual: ConfiguracionComercio,
    onDismiss: () -> Unit,
    onActivar: (clave: String, titular: String) -> Unit
) {
    var claveInput by remember { mutableStateOf(configActual.claveLicencia) }
    var titularInput by remember { mutableStateOf(configActual.titularLicencia) }

    val clavesSugeridas = listOf(
        "ANGEL-CAFE-PRO-2026",
        "SAZON-REST-UNLIMITED",
        "CAFE-PREMIUM-OFFLINE"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Activar Licencia Comercial", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Introduce tu serial o clave de activación comercial. Al validarse, el sistema quedará licenciado permanentemente para tu negocio sin caducidad.",
                    fontSize = 12.sp,
                    color = SoftGray
                )

                OutlinedTextField(
                    value = titularInput,
                    onValueChange = { titularInput = it },
                    label = { Text("Nombre del Titular o Razón Social *") },
                    placeholder = { Text("Ej: Angel's Cafe C.A.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("licencia_titular_input")
                )

                OutlinedTextField(
                    value = claveInput,
                    onValueChange = { claveInput = it },
                    label = { Text("Serial o Clave de Licencia *") },
                    placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("licencia_clave_input")
                )

                Text("Claves de activación rápida disponibles:", fontSize = 11.sp, color = SoftGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    clavesSugeridas.forEach { k ->
                        SuggestionChip(
                            onClick = { claveInput = k },
                            label = { Text(k.take(12) + "...", fontSize = 10.sp) }
                        )
                    }
                }

                Surface(
                    color = SoftGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✓", fontSize = 14.sp, color = SoftGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Modalidad 100% Offline: No requiere internet continuo para validar su funcionamiento.",
                            fontSize = 11.sp,
                            color = SoftGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onActivar(claveInput, titularInput) },
                enabled = claveInput.isNotBlank() && titularInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                modifier = Modifier.testTag("licencia_confirm_button")
            ) {
                Text("Activar Licencia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIALOG: CONFIRMACIÓN DE HARD RESET
@Composable
private fun HardResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: (conDatosEjemplo: Boolean) -> Unit
) {
    var confirmText by remember { mutableStateOf("") }
    var cargarDatosEjemplo by remember { mutableStateOf(true) }

    val palabraClave = "RESET"
    val esPalabraCorrecta = confirmText.trim().equals(palabraClave, ignoreCase = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(36.dp)) },
        title = {
            Text("¿Confirmar Hard Reset Total?", fontWeight = FontWeight.Bold, color = SoftRed, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Esta acción BORRARÁ TODO el contenido de la base de datos:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = SoftRed.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Clientes y cuentas por cobrar (CXC)", fontSize = 11.sp, color = SoftRed)
                        Text("• Proveedores y cuentas por pagar (CXP)", fontSize = 11.sp, color = SoftRed)
                        Text("• Historial de ventas y transacciones", fontSize = 11.sp, color = SoftRed)
                        Text("• Inventario de Materia Prima y Producto Terminado", fontSize = 11.sp, color = SoftRed)
                        Text("• Recetas estipuladas y movimientos de stock", fontSize = 11.sp, color = SoftRed)
                    }
                }

                // Opción para restaurar con datos de ejemplo o completamente en blanco
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { cargarDatosEjemplo = !cargarDatosEjemplo },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = cargarDatosEjemplo,
                        onCheckedChange = { cargarDatosEjemplo = it },
                        colors = CheckboxDefaults.colors(checkedColor = CafeDarkBrown)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recargar datos de demostración limpios (platos, materias primas y recetas de cafetería)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Para prevenir accidentes, escribe exactamente la palabra \"$palabraClave\" para autorizar el reinicio:",
                    fontSize = 12.sp,
                    color = SoftGray
                )

                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    placeholder = { Text(palabraClave) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftRed,
                        unfocusedBorderColor = SoftRed.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hard_reset_confirm_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cargarDatosEjemplo) },
                enabled = esPalabraCorrecta,
                colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                modifier = Modifier.testTag("hard_reset_execute_button")
            ) {
                Text("Confirmar y Borrar Todo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
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
