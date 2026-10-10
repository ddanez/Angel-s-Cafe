package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaccion
import com.example.ui.SazonViewModel
import com.example.ui.components.EditarTasaDialog
import com.example.ui.navigation.AppModule
import com.example.ui.theme.*
import com.example.ui.util.FiltroPeriodo
import com.example.ui.util.FormatUtils
import com.example.ui.util.SelectorPeriodoBar
import com.example.ui.util.TipoPeriodo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: SazonViewModel,
    onNavigateToModule: (AppModule) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val proveedores by viewModel.proveedoresConSaldo.collectAsState()
    val transacciones by viewModel.transacciones.collectAsState()
    val compras by viewModel.compras.collectAsState()
    val articulos by viewModel.articulosInventario.collectAsState()
    val actualizandoTasa by viewModel.actualizandoTasa.collectAsState()
    val mensajeTasa by viewModel.mensajeTasa.collectAsState()

    var showEditarTasaDialog by remember { mutableStateOf(false) }

    // Selector de Período temporal para métricas del Dashboard
    var filtroPeriodoDashboard by remember { mutableStateOf(FiltroPeriodo.porDefecto(TipoPeriodo.DIA)) }

    LaunchedEffect(mensajeTasa) {
        mensajeTasa?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val articulosBajoStock = remember(articulos) { articulos.filter { it.stockActual <= it.stockMinimo } }

    // Financial calculations
    val totalCxC = remember(clientes) {
        clientes.sumOf { it.saldoPendiente }
    }
    val totalCxp = remember(proveedores) {
        proveedores.sumOf { it.saldoPendienteCXP }
    }

    val transaccionesPeriodo = remember(transacciones, filtroPeriodoDashboard) {
        transacciones.filter { filtroPeriodoDashboard.coincide(it.fecha) }
    }
    val ventasPeriodo = remember(transaccionesPeriodo) {
        transaccionesPeriodo
            .filter { it.tipo in listOf("VENTA_CONTADO", "COMPRA") }
            .sumOf { it.montoTotal }
    }
    val recaudadoPeriodo = remember(transaccionesPeriodo) {
        transaccionesPeriodo
            .filter { it.tipo == "ABONO" }
            .sumOf { it.montoTotal }
    }
    val comprasMes = remember(compras) {
        val mesActual = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
        compras
            .filter { SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date(it.fecha)) == mesActual }
            .sumOf { it.montoTotal }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Banner de Licencia Demo (15 días de prueba)
        if (config.planLicencia == "DEMO") {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoldenCrema.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldenCrema),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToModule(AppModule.AJUSTES) }
                        .testTag("dashboard_demo_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("⏳", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Licencia Demo: ${config.diasRestantesLicencia()} días restantes (de 15)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vence: ${FormatUtils.formatDateOnly(config.fechaVencimientoLicencia)}. Toque para activar licencia comercial.",
                                    fontSize = 11.sp,
                                    color = SoftGray
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Activar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Welcome Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CafeDarkBrown),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_welcome_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = config.nombreComercio,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SmoothBeige
                            )
                            Text(
                                text = "Panel de Control Operativo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoldenCrema
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CafeBrown.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☕", fontSize = 26.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ventas (${filtroPeriodoDashboard.tipo.titulo})", style = MaterialTheme.typography.labelMedium, color = SoftGray)
                            Text(
                                FormatUtils.formatCurrency(ventasPeriodo, config.monedaSimbolo),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SmoothBeige
                            )
                            Text(
                                FormatUtils.formatBs(ventasPeriodo, config.tasaCambioBs),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldenCrema
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cobrado (${filtroPeriodoDashboard.tipo.titulo})", style = MaterialTheme.typography.labelMedium, color = SoftGray)
                            Text(
                                FormatUtils.formatCurrency(recaudadoPeriodo, config.monedaSimbolo),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SoftGreen
                            )
                            Text(
                                FormatUtils.formatBs(recaudadoPeriodo, config.tasaCambioBs),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreen.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SelectorPeriodoBar(
                        filtro = filtroPeriodoDashboard,
                        onFiltroCambiado = { filtroPeriodoDashboard = it }
                    )
                }
            }
        }

        // Tasa del Día Oficial Banner
        item {
            Card(
                onClick = { showEditarTasaDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_tasa_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("💵", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tasa Oficial BCV: 1 USD = Bs. ${String.format(java.util.Locale.US, "%.2f", config.tasaCambioBs)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (actualizandoTasa) "Sincronizando con el BCV..." else "Conversión dual activa • Toca para ajustar",
                                fontSize = 10.sp,
                                color = if (actualizandoTasa) MaterialTheme.colorScheme.primary else SoftGray
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showEditarTasaDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Ajustar tasa manualmente",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.actualizarTasaDesdeInternet(forzar = true) },
                            enabled = !actualizandoTasa,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (actualizandoTasa) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sincronizar BCV ahora", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }


        // Key Financial KPI Cards
        item {
            Text(
                text = "Cuentas y Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CXC Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToModule(AppModule.CXC) }
                        .testTag("kpi_cxc_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Por Cobrar (CXC)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SoftRed)
                            Text("💰", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FormatUtils.formatCurrency(totalCxC, config.monedaSimbolo),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = FormatUtils.formatBs(totalCxC, config.tasaCambioBs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${clientes.count { it.saldoPendiente > 0 }} clientes con deuda",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // CXP Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToModule(AppModule.CXP) }
                        .testTag("kpi_cxp_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Por Pagar (CXP)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SoftRed)
                            Text("📑", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FormatUtils.formatCurrency(totalCxp, config.monedaSimbolo),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = FormatUtils.formatBs(totalCxp, config.tasaCambioBs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${proveedores.count { it.saldoPendienteCXP > 0 }} proveedores pendientes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }


        // Quick Access Modules Grid
        item {
            Text(
                text = "Módulos del Sistema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickModuleButton(
                    module = AppModule.VENTAS,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.VENTAS) }
                )
                QuickModuleButton(
                    module = AppModule.INVENTARIO,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.INVENTARIO) }
                )
                QuickModuleButton(
                    module = AppModule.COMPRAS,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.COMPRAS) }
                )
                QuickModuleButton(
                    module = AppModule.CLIENTES,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.CLIENTES) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickModuleButton(
                    module = AppModule.PROVEEDORES,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.PROVEEDORES) }
                )
                QuickModuleButton(
                    module = AppModule.CXC,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.CXC) }
                )
                QuickModuleButton(
                    module = AppModule.CXP,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.CXP) }
                )
                QuickModuleButton(
                    module = AppModule.REPORTES,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToModule(AppModule.REPORTES) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickModuleButton(
                    module = AppModule.AJUSTES,
                    modifier = Modifier.fillMaxWidth(0.5f),
                    onClick = { onNavigateToModule(AppModule.AJUSTES) }
                )
            }
        }

        // Recent Movements Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos Movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { onNavigateToModule(AppModule.REPORTES) }) {
                    Text("Ver Reportes", color = CafeBrown)
                }
            }
        }

        if (transacciones.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay transacciones registradas.\nUsa el módulo de Ventas o Clientes para empezar.",
                            color = SoftGray,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(transacciones.take(6)) { tx ->
                val clienteNombre = remember(tx.clienteId, clientes) {
                    clientes.find { it.cliente.id == tx.clienteId }?.cliente?.nombre ?: "Venta de Mostrador"
                }
                RecentTxCard(tx = tx, clienteNombre = clienteNombre, moneda = config.monedaSimbolo)
            }
        }
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
private fun QuickModuleButton(
    module: AppModule,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("quick_${module.tag}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(module.iconEmoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = module.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentTxCard(
    tx: Transaccion,
    clienteNombre: String,
    moneda: String
) {
    val isAbono = tx.tipo == "ABONO"
    val isContado = tx.tipo == "VENTA_CONTADO"
    val tipoLabel = when (tx.tipo) {
        "ABONO" -> "Abono / Pago Recibido"
        "VENTA_CONTADO" -> "Venta al Contado"
        else -> "Venta a Crédito (CXC)"
    }
    val badgeColor = when (tx.tipo) {
        "ABONO" -> SoftGreen
        "VENTA_CONTADO" -> GoldenCrema
        else -> SoftRed
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAbono) "💵" else if (isContado) "⚡" else "📋",
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clienteNombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tx.detalle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    text = FormatUtils.formatDateShort(tx.fecha),
                    fontSize = 10.sp,
                    color = SoftGray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isAbono) "+ " else "") + FormatUtils.formatCurrency(tx.montoTotal, moneda),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isAbono) SoftGreen else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tipoLabel,
                    fontSize = 10.sp,
                    color = badgeColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
