package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var showEditarTasaDialog by remember { mutableStateOf(false) }

    val actualizandoTasa by viewModel.actualizandoTasa.collectAsState()
    val mensajeTasa by viewModel.mensajeTasa.collectAsState()

    LaunchedEffect(mensajeTasa) {
        mensajeTasa?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.limpiarMensajeTasa()
        }
    }

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

        // Section: TASA DE CAMBIO (USD & BOLÍVARES)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("card_tasa_cambio")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💵", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Tasa de Cambio Oficial (USD / Bs)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Todos los precios del sistema se calculan en Dólares ($) y Bolívares (Bs.)",
                                    fontSize = 11.sp,
                                    color = SoftGray
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tasa actual de conversión:", fontSize = 11.sp, color = SoftGray)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "1 USD = ",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Bs. ${String.format(java.util.Locale.US, "%.2f", config.tasaCambioBs)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CafeDarkBrown
                                    )
                                }
                                Text(
                                    text = "Última actualización: ${FormatUtils.formatDate(config.fechaActualizacionTasa)}",
                                    fontSize = 10.sp,
                                    color = SoftGray
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showEditarTasaDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manual", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.actualizarTasaDesdeInternet(forzar = true) },
                                    enabled = !actualizandoTasa,
                                    colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (actualizandoTasa) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SmoothBeige, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Actualizar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAutoActualizarTasa(!config.autoActualizarTasa) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = config.autoActualizarTasa,
                            onCheckedChange = { viewModel.toggleAutoActualizarTasa(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CafeDarkBrown)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Actualización Diaria Automática", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Sincroniza la tasa automáticamente todos los días mediante el servicio oficial en línea.", fontSize = 11.sp, color = SoftGray)
                        }
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
                    Text("${platos.size} platos registrados para la venta (con doble precio USD/Bs)", fontSize = 12.sp, color = SoftGray)
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
                            text = FormatUtils.formatDual(plato.precio, config.tasaCambioBs, config.monedaSimbolo),
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

                        val esDemo = config.planLicencia == "DEMO"
                        val esActiva = config.fueActivadaConClave && !config.estaBloqueadaPorLicencia()

                        Surface(
                            color = when {
                                esActiva -> SoftGreen.copy(alpha = 0.18f)
                                esDemo -> GoldenCrema.copy(alpha = 0.35f)
                                else -> SoftRed.copy(alpha = 0.18f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    esActiva -> "✓ COMERCIAL ACTIVA"
                                    esDemo -> "DEMO (15 DÍAS)"
                                    else -> "⚠️ BLOQUEADA / EXPIRADA"
                                },
                                color = when {
                                    esActiva -> SoftGreen
                                    esDemo -> CafeDarkBrown
                                    else -> SoftRed
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (config.planLicencia == "DEMO")
                            "Licencia Demo de evaluación inicial (15 días de prueba). Al finalizar se bloqueará el acceso hasta ingresar la clave de autorización."
                        else "Licencia comercial activa y autorizada para este terminal de punto de venta.",
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Plan Actual:", fontSize = 12.sp, color = SoftGray)
                                Surface(
                                    color = if (config.planLicencia == "DEMO") GoldenCrema.copy(alpha = 0.35f) else SoftGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = config.planLicencia,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (config.planLicencia == "DEMO") CafeDarkBrown else SoftGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (config.planLicencia == "DEMO") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Período de Prueba:", fontSize = 12.sp, color = SoftGray)
                                    Text(
                                        "${config.diasRestantesLicencia()} días restantes (de 15)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (config.diasRestantesLicencia() <= 3) SoftRed else CafeDarkBrown
                                    )
                                }
                            }

                            if (config.fechaVencimientoLicencia > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fecha Límite / Vencimiento:", fontSize = 12.sp, color = SoftGray)
                                    Text(
                                        FormatUtils.formatDateOnly(config.fechaVencimientoLicencia),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (config.estaBloqueadaPorLicencia()) SoftRed else CafeBrown
                                    )
                                }
                            } else if (config.planLicencia == "VITALICIA" && config.fueActivadaConClave) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Vigencia:", fontSize = 12.sp, color = SoftGray)
                                    Text("Permanente (Sin fecha de caducidad)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SoftGreen)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estado de Activación:", fontSize = 12.sp, color = SoftGray)
                                Text(
                                    text = if (config.fueActivadaConClave) "✓ Autorizado con Clave" else "Prueba Demo Inicial",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (config.fueActivadaConClave) SoftGreen else SoftGray
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (config.planLicencia == "DEMO") {
                            TextButton(
                                onClick = { viewModel.simularVencimientoLicencia() },
                                colors = ButtonDefaults.textButtonColors(contentColor = SoftRed)
                            ) {
                                Text("Probar Bloqueo (Simular 15d)", fontSize = 11.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = { showLicenciaDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (config.planLicencia == "DEMO") "Activar Licencia Comercial" else "Modificar / Renovar Licencia",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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
            onActivar = { clave, plan, titular ->
                viewModel.activarLicenciaPlan(clave, plan, titular) { exito, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (exito) showLicenciaDialog = false
                }
            }
        )
    }

    // --- DIÁLOGO DE MODIFICACIÓN MANUAL DE TASA DE CAMBIO ---
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

// DIALOG: ACTIVACIÓN / GESTIÓN DE LICENCIA (1 Mes, Semestral, Anual o Vitalicio con Clave Secreta)
@Composable
private fun LicenciaDialog(
    configActual: ConfiguracionComercio,
    onDismiss: () -> Unit,
    onActivar: (clave: String, plan: String, titular: String) -> Unit
) {
    var claveInput by remember { mutableStateOf("") }
    var titularInput by remember { mutableStateOf(configActual.titularLicencia) }
    var selectedPlan by remember { mutableStateOf(if (configActual.planLicencia == "DEMO") "VITALICIA" else configActual.planLicencia) }

    val planes = listOf(
        Triple("MENSUAL", "1 Mes", "30 días de vigencia comercial"),
        Triple("SEMESTRAL", "Semestral", "6 meses (180 días)"),
        Triple("ANUAL", "Anual", "1 año (365 días)"),
        Triple("VITALICIA", "Vitalicio", "Acceso permanente sin caducidad")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Licenciamiento del Sistema", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Selecciona el período de licenciamiento comercial deseado e ingresa la clave de autorización provista por su desarrollador para validar la activación.",
                        fontSize = 12.sp,
                        color = SoftGray
                    )
                }

                item {
                    Text("Período de Licencia:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CafeDarkBrown)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        planes.forEach { (id, label, desc) ->
                            val isSelected = selectedPlan == id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CafeBrown.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CafeDarkBrown) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlan = id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlan = id },
                                        colors = RadioButtonDefaults.colors(selectedColor = CafeDarkBrown)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(desc, fontSize = 11.sp, color = SoftGray)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = titularInput,
                        onValueChange = { titularInput = it },
                        label = { Text("Titular del Negocio / Razón Social *") },
                        placeholder = { Text("Ej: Angel's Cafe C.A.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("licencia_titular_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = claveInput,
                        onValueChange = { claveInput = it },
                        label = { Text("Clave de Autorización *") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        supportingText = { Text("Ingrese la clave secreta provista por el desarrollador para autorizar este plan") },
                        modifier = Modifier.fillMaxWidth().testTag("licencia_clave_input")
                    )
                }

                item {
                    Surface(
                        color = SoftGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("✓", fontSize = 14.sp, color = SoftGreen, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Funcionamiento 100% Offline garantizado durante la vigencia de la licencia.",
                                fontSize = 11.sp,
                                color = SoftGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onActivar(claveInput, selectedPlan, titularInput) },
                enabled = claveInput.isNotBlank() && titularInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                modifier = Modifier.testTag("licencia_confirm_button")
            ) {
                Text("Activar Licencia ($selectedPlan)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIALOG: AJUSTE MANUAL DE LA TASA DE CAMBIO
@Composable
private fun EditarTasaDialog(
    tasaActual: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var tasaInput by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", tasaActual)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💵", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fijar Tasa de Cambio Manual", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ingresa el valor en Bolívares (Bs.) por cada 1 USD. Todos los módulos, tickets, inventarios y reportes recalcularán automáticamente sus equivalencias.",
                    fontSize = 12.sp,
                    color = SoftGray
                )

                OutlinedTextField(
                    value = tasaInput,
                    onValueChange = { tasaInput = it },
                    label = { Text("Tasa de Cambio (Bs. por 1 USD) *") },
                    placeholder = { Text("Ej: 54.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("tasa_cambio_manual_input")
                )

                Text(
                    "Ejemplos rápidos:",
                    fontSize = 11.sp,
                    color = SoftGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(45.0, 50.0, 54.5, 60.0).forEach { r ->
                        SuggestionChip(
                            onClick = { tasaInput = String.format(java.util.Locale.US, "%.2f", r) },
                            label = { Text("Bs. $r", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = tasaInput.replace(",", ".").toDoubleOrNull()
                    if (rate != null && rate > 0) {
                        onConfirm(rate)
                    }
                },
                enabled = (tasaInput.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown)
            ) {
                Text("Guardar Tasa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIALOG: CONFIRMACIÓN DE HARD RESET (Restablecimiento Total de Fábrica)
@Composable
private fun HardResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: (conDatosEjemplo: Boolean) -> Unit
) {
    var confirmText by remember { mutableStateOf("") }
    var cargarDatosEjemplo by remember { mutableStateOf(true) }

    val palabraClave = "RESET"
    // Validación flexible: acepta RESET o BORRAR en mayúsculas o minúsculas
    val esPalabraCorrecta = confirmText.trim().equals(palabraClave, ignoreCase = true) ||
            confirmText.trim().equals("BORRAR", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(36.dp)) },
        title = {
            Text(
                text = "⚠️ Confirmar Hard Reset Total",
                fontWeight = FontWeight.Bold,
                color = SoftRed,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Esta acción eliminará de forma irreversible los datos del sistema:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = SoftRed.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftRed.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("• Ventas, pedidos e historial comercial", fontSize = 11.sp, color = SoftRed)
                        Text("• Cuentas por cobrar (CXC) y por pagar (CXP)", fontSize = 11.sp, color = SoftRed)
                        Text("• Clientes y Proveedores registrados", fontSize = 11.sp, color = SoftRed)
                        Text("• Inventario doble (Materias Primas y Vitrina)", fontSize = 11.sp, color = SoftRed)
                        Text("• Recetas estipuladas y movimientos de stock", fontSize = 11.sp, color = SoftRed)
                    }
                }

                // Opción para restaurar con datos de ejemplo o en blanco
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { cargarDatosEjemplo = !cargarDatosEjemplo },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = cargarDatosEjemplo,
                        onCheckedChange = { cargarDatosEjemplo = it },
                        colors = CheckboxDefaults.colors(checkedColor = CafeDarkBrown)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recargar datos de ejemplo de cafetería tras el reinicio",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // RECUADRO DESTACADO CON LA PALABRA DE SEGURIDAD
                Card(
                    colors = CardDefaults.cardColors(containerColor = SoftRed.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SoftRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PALABRA DE SEGURIDAD REQUERIDA:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = palabraClave,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            color = SoftRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Escribe la palabra RESET para desbloquear el botón",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                // CAMPO DE TEXTO PARA ESCRIBIR LA PALABRA DE SEGURIDAD
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    label = { Text("Escribe la palabra: RESET") },
                    placeholder = { Text("Escribe RESET aquí...") },
                    singleLine = true,
                    trailingIcon = {
                        if (esPalabraCorrecta) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Correcto", tint = SoftGreen)
                        }
                    },
                    supportingText = {
                        if (esPalabraCorrecta) {
                            Text("✓ Palabra correcta. Ya puedes confirmar el reinicio.", color = SoftGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        } else {
                            Text("Debes escribir RESET (en mayúsculas o minúsculas)", color = SoftRed, fontSize = 11.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (esPalabraCorrecta) SoftGreen else SoftRed,
                        unfocusedBorderColor = if (esPalabraCorrecta) SoftGreen else SoftRed.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hard_reset_confirm_input")
                )

                // Botón rápido para autocompletar si se desea
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { confirmText = palabraClave },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = SoftRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Autocompletar 'RESET'", fontSize = 11.sp, color = SoftRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cargarDatosEjemplo) },
                enabled = esPalabraCorrecta,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftRed,
                    disabledContainerColor = SoftRed.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("hard_reset_execute_button")
            ) {
                Text(
                    text = if (esPalabraCorrecta) "Confirmar y Borrar Todo" else "Escribe RESET para Confirmar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
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
