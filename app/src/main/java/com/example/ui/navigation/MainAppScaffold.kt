package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SazonViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var currentModule by remember { mutableStateOf(AppModule.DASHBOARD) }

    val config by viewModel.configuracion.collectAsState()
    val clientes by viewModel.clientesConSaldo.collectAsState()
    val proveedores by viewModel.proveedoresConSaldo.collectAsState()
    val carrito by viewModel.carrito.collectAsState()
    val articulos by viewModel.articulosInventario.collectAsState()

    val deudoresCount = remember(clientes) { clientes.count { it.saldoPendiente > 0.0 } }
    val proveedoresDeudaCount = remember(proveedores) { proveedores.count { it.saldoPendienteCXP > 0.0 } }
    val carritoCount = remember(carrito) { carrito.values.sum() }
    val articulosBajoStockCount = remember(articulos) { articulos.count { it.stockActual <= it.stockMinimo } }

    // Intercept back button:
    // 1. If drawer is open -> close it
    // 2. If on sub-module -> return to DASHBOARD
    BackHandler(enabled = drawerState.isOpen || currentModule != AppModule.DASHBOARD) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            currentModule = AppModule.DASHBOARD
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .testTag("modal_navigation_drawer_sheet"),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Header del menú lateral
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CafeDarkBrown),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☕", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = config.nombreComercio,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Menú Principal",
                                style = MaterialTheme.typography.bodySmall,
                                color = CafeBrown,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "MÓDULOS DEL SISTEMA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // Lista de los 9 módulos con autocolapso
                    AppModule.values().forEach { module ->
                        val isSelected = currentModule == module
                        val badgeText = when (module) {
                            AppModule.CXC -> if (deudoresCount > 0) "$deudoresCount" else null
                            AppModule.CXP -> if (proveedoresDeudaCount > 0) "$proveedoresDeudaCount" else null
                            AppModule.VENTAS -> if (carritoCount > 0) "$carritoCount" else null
                            AppModule.INVENTARIO -> if (articulosBajoStockCount > 0) "!$articulosBajoStockCount" else null
                            else -> null
                        }
                        val badgeColor = when (module) {
                            AppModule.CXC, AppModule.CXP, AppModule.INVENTARIO -> SoftRed
                            AppModule.VENTAS -> GoldenCrema
                            else -> CafeBrown
                        }

                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = module.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            },
                            icon = {
                                Text(text = module.iconEmoji, fontSize = 20.sp)
                            },
                            badge = badgeText?.let {
                                {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = it,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (module == AppModule.VENTAS) DarkText else LightText,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                currentModule = module
                                // AUTOCOLAPSABLE: Cierra el menú lateral automáticamente al presionar una opción
                                coroutineScope.launch {
                                    drawerState.close()
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = CafeBrown.copy(alpha = 0.18f),
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .testTag(module.tag)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Footer informativo
                    Text(
                        text = "Angel's Cafe v2.0 • Offline Local",
                        fontSize = 11.sp,
                        color = SoftGray,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentModule.iconEmoji,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentModule == AppModule.DASHBOARD) config.nombreComercio else currentModule.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .testTag("hamburger_menu_button")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CafeDarkBrown,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Abrir menú lateral",
                                        tint = SmoothBeige,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (currentModule != AppModule.DASHBOARD) {
                            IconButton(
                                onClick = { currentModule = AppModule.DASHBOARD },
                                modifier = Modifier.testTag("topbar_home_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Ir a Dashboard",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else if (carritoCount > 0) {
                            IconButton(
                                onClick = { currentModule = AppModule.VENTAS },
                                modifier = Modifier.testTag("topbar_cart_shortcut")
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = GoldenCrema) {
                                            Text("$carritoCount", color = DarkText, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Ir a Ventas / Carrito",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentModule) {
                    AppModule.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToModule = { target -> currentModule = target },
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                    AppModule.PROVEEDORES -> ProveedoresScreen(viewModel = viewModel)
                    AppModule.CLIENTES -> ClientesScreen(viewModel = viewModel)
                    AppModule.COMPRAS -> ComprasScreen(viewModel = viewModel)
                    AppModule.INVENTARIO -> InventarioScreen(viewModel = viewModel)
                    AppModule.VENTAS -> VentasScreen(viewModel = viewModel)
                    AppModule.CXC -> CxcScreen(viewModel = viewModel)
                    AppModule.CXP -> CxpScreen(viewModel = viewModel)
                    AppModule.REPORTES -> ReportesScreen(viewModel = viewModel)
                    AppModule.AJUSTES -> AjustesScreen(viewModel = viewModel)
                }
            }
        }
    }
}
