package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArticuloInventario
import com.example.data.model.MovimientoInventario
import com.example.data.model.RecetaIngrediente
import com.example.ui.SazonViewModel
import com.example.ui.theme.*
import com.example.ui.util.FiltroPeriodo
import com.example.ui.util.FormatUtils
import com.example.ui.util.SelectorPeriodoBar
import com.example.ui.util.TipoPeriodo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventarioScreen(
    viewModel: SazonViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configuracion.collectAsState()
    val articulos by viewModel.articulosInventario.collectAsState()
    val movimientos by viewModel.movimientosInventario.collectAsState()
    val recetas by viewModel.recetasIngredientes.collectAsState()

    // 0: Materia Prima, 1: Producto Terminado, 2: Historial Movimientos
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Todos") }
    var soloBajoStock by remember { mutableStateOf(false) }

    // Filtro temporal para el Kárdex de movimientos
    var filtroPeriodoKardex by remember { mutableStateOf(FiltroPeriodo.porDefecto(TipoPeriodo.MES)) }

    // Dialog state
    var showAddArticleDialog by remember { mutableStateOf(false) }
    var initialTipoForNewArticle by remember { mutableStateOf("MATERIA_PRIMA") }
    var articleToEdit by remember { mutableStateOf<ArticuloInventario?>(null) }
    var articleToDelete by remember { mutableStateOf<ArticuloInventario?>(null) }
    var articleForMovement by remember { mutableStateOf<ArticuloInventario?>(null) }
    var productForRecipe by remember { mutableStateOf<ArticuloInventario?>(null) }
    var productForElaborate by remember { mutableStateOf<ArticuloInventario?>(null) }

    val materiasPrimas = remember(articulos) {
        articulos.filter { it.tipoInventario == "MATERIA_PRIMA" }
    }
    val productosTerminados = remember(articulos) {
        articulos.filter { it.tipoInventario == "PRODUCTO_TERMINADO" }
    }

    val articulosCurrentTab = remember(selectedTab, articulos) {
        when (selectedTab) {
            0 -> materiasPrimas
            1 -> productosTerminados
            else -> articulos
        }
    }

    // Categorías disponibles según tab actual
    val categorias = remember(articulosCurrentTab) {
        val cats = articulosCurrentTab.map { it.categoria }.distinct().filter { it.isNotBlank() }
        listOf("Todos") + cats
    }

    val articulosBajoStockMP = remember(materiasPrimas) {
        materiasPrimas.filter { it.stockActual <= it.stockMinimo }
    }
    val articulosBajoStockPT = remember(productosTerminados) {
        productosTerminados.filter { it.stockActual <= it.stockMinimo }
    }

    val valorTotalMP = remember(materiasPrimas) {
        materiasPrimas.sumOf { it.stockActual * it.costoUnitario }
    }
    val valorTotalPT = remember(productosTerminados) {
        productosTerminados.sumOf { it.stockActual * it.costoUnitario }
    }

    val filteredArticulos = remember(articulosCurrentTab, searchQuery, selectedCategoryFilter, soloBajoStock) {
        articulosCurrentTab.filter { art ->
            val matchSearch = searchQuery.isBlank() ||
                    art.nombre.contains(searchQuery, ignoreCase = true) ||
                    art.codigo.contains(searchQuery, ignoreCase = true) ||
                    art.categoria.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategoryFilter == "Todos" || art.categoria.equals(selectedCategoryFilter, ignoreCase = true)
            val matchBajoStock = !soloBajoStock || (art.stockActual <= art.stockMinimo)
            matchSearch && matchCategory && matchBajoStock
        }
    }

    val filteredMovimientos = remember(movimientos, searchQuery, filtroPeriodoKardex) {
        movimientos
            .filter { filtroPeriodoKardex.coincide(it.fecha) }
            .filter {
                if (searchQuery.isBlank()) true
                else it.articuloNombre.contains(searchQuery, ignoreCase = true) ||
                        it.motivo.contains(searchQuery, ignoreCase = true) ||
                        it.tipo.contains(searchQuery, ignoreCase = true)
            }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        initialTipoForNewArticle = if (selectedTab == 0) "MATERIA_PRIMA" else "PRODUCTO_TERMINADO"
                        showAddArticleDialog = true
                    },
                    containerColor = CafeDarkBrown,
                    contentColor = SmoothBeige,
                    modifier = Modifier.testTag("add_inventario_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Artículo")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // Header KPIs con distinción clara de los 2 inventarios
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KPI: Materia Prima
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 0
                                soloBajoStock = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTab == 0) CafeBrown.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Materia Prima", fontSize = 11.sp, color = CafeBrown, fontWeight = FontWeight.Bold)
                                Text("🌾", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${materiasPrimas.size} insumos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (articulosBajoStockMP.isNotEmpty()) "⚠️ ${articulosBajoStockMP.size} bajo stock" else "Stock regular",
                                fontSize = 10.sp,
                                color = if (articulosBajoStockMP.isNotEmpty()) SoftRed else SoftGreen
                            )
                        }
                    }

                    // KPI: Producto Terminado
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 1
                                soloBajoStock = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTab == 1) CafeBrown.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Prod. Terminado", fontSize = 11.sp, color = CafeDarkBrown, fontWeight = FontWeight.Bold)
                                Text("🥐", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${productosTerminados.size} para vitrina",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (articulosBajoStockPT.isNotEmpty()) "⚠️ ${articulosBajoStockPT.size} bajo stock" else "Vitrina surtida",
                                fontSize = 10.sp,
                                color = if (articulosBajoStockPT.isNotEmpty()) SoftRed else SoftGreen
                            )
                        }
                    }

                    // KPI: Valorización Total
                    Card(
                        modifier = Modifier.weight(1.1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Valorización Stock", fontSize = 11.sp, color = SoftGray, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            val totalValor = valorTotalMP + valorTotalPT
                            Text(
                                FormatUtils.formatCurrency(totalValor, config.monedaSimbolo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                FormatUtils.formatBs(totalValor, config.tasaCambioBs),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CafeDarkBrown
                            )
                        }
                    }
                }
            }

            // Explicativo Banner de Proceso (Materia Prima -> Receta -> Producto Terminado)
            item {
                Surface(
                    color = CafeBrown.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gestión con Descuento Automático",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CafeDarkBrown
                            )
                            Text(
                                text = "Al preparar empanadas, arepas o café para la vitrina, el botón 'Elaborar' descuenta los insumos de materia prima según su receta.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Tabs Principales: 1. Materia Prima | 2. Producto Terminado | 3. Historial
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = CafeDarkBrown,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            selectedCategoryFilter = "Todos"
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌾 Materia Prima (${materiasPrimas.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("inventario_tab_materia_prima")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedCategoryFilter = "Todos"
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🥐 Prod. Terminado (${productosTerminados.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("inventario_tab_producto_terminado")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📋 Historial (${movimientos.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("inventario_tab_movimientos")
                    )
                }
            }

            // Search & Category Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventario_search_input"),
                        placeholder = {
                            Text(
                                when (selectedTab) {
                                    0 -> "Buscar harina, carne, leche, café en grano..."
                                    1 -> "Buscar empanadas, arepas, vitrina..."
                                    else -> "Buscar en historial de movimientos..."
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = CafeBrown)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CafeBrown,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )

                    // Filtros por Categoría y Bajo Stock en Tab 0 y 1
                    if (selectedTab == 0 || selectedTab == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = soloBajoStock,
                                onClick = { soloBajoStock = !soloBajoStock },
                                label = {
                                    Text(if (soloBajoStock) "⚠️ Solo Bajo Stock ✓" else "⚠️ Bajo Stock", fontSize = 11.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftRed.copy(alpha = 0.2f),
                                    selectedLabelColor = SoftRed
                                )
                            )

                            categorias.take(3).forEach { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CafeDarkBrown,
                                        selectedLabelColor = SmoothBeige
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // CONTENIDO DE LAS PESTAÑAS
            if (selectedTab == 0 || selectedTab == 1) {
                // Listado de Artículos
                if (filteredArticulos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(if (selectedTab == 0) "🌾" else "🥐", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTab == 0) "Sin Materia Prima Registrada" else "Sin Productos Terminados Registrados",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (selectedTab == 0)
                                        "Registra los insumos comprados a proveedores (harina, café en grano, carne, queso, leche) con el botón +"
                                    else
                                        "Registra lo elaborado para la vitrina (empanadas, arepas, bollitos, café) y configúrales su receta.",
                                    fontSize = 13.sp,
                                    color = SoftGray,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                if (soloBajoStock) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(onClick = { soloBajoStock = false }) {
                                        Text("Mostrar todos los artículos", color = CafeDarkBrown)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredArticulos, key = { it.id }) { articulo ->
                        val ingredientesProducto = remember(recetas, articulo.id) {
                            recetas.filter { it.productoTerminadoId == articulo.id }
                        }

                        ArticuloDualCard(
                            articulo = articulo,
                            monedaSimbolo = config.monedaSimbolo,
                            tasaCambioBs = config.tasaCambioBs,
                            ingredientesReceta = ingredientesProducto,
                            onAjustarStock = { articleForMovement = articulo },
                            onEditar = { articleToEdit = articulo },
                            onBorrar = { articleToDelete = articulo },
                            onEditarReceta = { productForRecipe = articulo },
                            onElaborarParaVitrina = { productForElaborate = articulo }
                        )
                    }
                }
            } else {
                // Tab 2: Historial de Movimientos / Kárdex con filtro por período
                item {
                    SelectorPeriodoBar(
                        filtro = filtroPeriodoKardex,
                        onFiltroCambiado = { filtroPeriodoKardex = it }
                    )
                }

                if (filteredMovimientos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📋", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Sin movimientos registrados", fontWeight = FontWeight.Bold)
                                Text("Los registros de entradas, salidas, elaboración y ajustes aparecerán aquí.", fontSize = 13.sp, color = SoftGray)
                            }
                        }
                    }
                } else {
                    items(filteredMovimientos, key = { it.id }) { mov ->
                        MovimientoInventarioCard(
                            movimiento = mov,
                            onBorrar = { viewModel.borrarMovimientoInventario(mov) }
                        )
                    }
                }
            }
        }
    }

    // DIALOGS

    // 1. Agregar Artículo (Materia Prima o Producto Terminado)
    if (showAddArticleDialog) {
        ArticuloFormDialog(
            title = if (initialTipoForNewArticle == "MATERIA_PRIMA") "Nueva Materia Prima / Insumo" else "Nuevo Producto Terminado (Vitrina)",
            initialTipo = initialTipoForNewArticle,
            monedaSimbolo = config.monedaSimbolo,
            onDismiss = { showAddArticleDialog = false },
            onConfirm = { codigo, nombre, tipo, cat, um, stock, sMin, costo, pVenta, notas ->
                viewModel.agregarArticuloInventario(codigo, nombre, tipo, cat, um, stock, sMin, costo, pVenta, notas)
                showAddArticleDialog = false
                Toast.makeText(context, "Artículo guardado exitosamente", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Editar Artículo
    articleToEdit?.let { art ->
        ArticuloFormDialog(
            title = "Editar Artículo",
            initialArticulo = art,
            initialTipo = art.tipoInventario,
            monedaSimbolo = config.monedaSimbolo,
            onDismiss = { articleToEdit = null },
            onConfirm = { codigo, nombre, tipo, cat, um, stock, sMin, costo, pVenta, notas ->
                viewModel.editarArticuloInventario(
                    art.copy(
                        codigo = codigo,
                        nombre = nombre,
                        tipoInventario = tipo,
                        categoria = cat,
                        unidadMedida = um,
                        stockActual = stock,
                        stockMinimo = sMin,
                        costoUnitario = costo,
                        precioVenta = pVenta,
                        notas = notas
                    )
                )
                articleToEdit = null
                Toast.makeText(context, "Artículo actualizado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Movimiento de Stock Directo (Entrada/Salida/Ajuste)
    articleForMovement?.let { art ->
        MovimientoStockDialog(
            articulo = art,
            onDismiss = { articleForMovement = null },
            onConfirm = { tipo, cantidad, motivo ->
                viewModel.registrarAjusteO_MovimientoStock(art, tipo, cantidad, motivo)
                articleForMovement = null
                Toast.makeText(context, "Stock actualizado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. Modal para Configurar Receta de Producto Terminado
    productForRecipe?.let { pt ->
        val currentIngredientes = remember(recetas, pt.id) {
            recetas.filter { it.productoTerminadoId == pt.id }
        }

        RecetaConfigDialog(
            productoTerminado = pt,
            materiasPrimasDisponibles = materiasPrimas,
            ingredientesActuales = currentIngredientes,
            onDismiss = { productForRecipe = null },
            onSave = { listaIngredientes ->
                viewModel.guardarRecetaIngredientes(pt.id, listaIngredientes)
                productForRecipe = null
                Toast.makeText(context, "Receta de ${pt.nombre} guardada", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Modal para Elaborar Producto Terminado para Vitrina (Rebaja de Materia Prima)
    productForElaborate?.let { pt ->
        val ingredientes = remember(recetas, pt.id) {
            recetas.filter { it.productoTerminadoId == pt.id }
        }

        ElaborarProductoDialog(
            productoTerminado = pt,
            ingredientesReceta = ingredientes,
            materiasPrimas = materiasPrimas,
            onDismiss = { productForElaborate = null },
            onConfirmElaborar = { cantidad, motivo ->
                viewModel.elaborarProductoParaVitrina(
                    productoTerminadoId = pt.id,
                    cantidad = cantidad,
                    motivo = motivo
                ) { errorMsg ->
                    if (errorMsg != null) {
                        Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            context,
                            "¡${cantidad.toInt()} ${pt.unidadMedida} pasadas a vitrina! Se descontó la materia prima.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                productForElaborate = null
            }
        )
    }

    // 6. Eliminar Artículo
    articleToDelete?.let { art ->
        AlertDialog(
            onDismissRequest = { articleToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SoftRed) },
            title = { Text("Eliminar Artículo") },
            text = { Text("¿Deseas eliminar permanentemente \"${art.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarArticuloInventario(art)
                        articleToDelete = null
                        Toast.makeText(context, "Artículo eliminado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ArticuloDualCard(
    articulo: ArticuloInventario,
    monedaSimbolo: String,
    tasaCambioBs: Double,
    ingredientesReceta: List<RecetaIngrediente>,
    onAjustarStock: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    onEditarReceta: () -> Unit,
    onElaborarParaVitrina: () -> Unit
) {
    val isBajoStock = articulo.stockActual <= articulo.stockMinimo
    val isProductoTerminado = articulo.tipoInventario == "PRODUCTO_TERMINADO"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("articulo_card_${articulo.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header del Artículo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isProductoTerminado) CafeDarkBrown.copy(alpha = 0.12f) else SoftGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isProductoTerminado) "🥐 Producto Terminado" else "🌾 Materia Prima",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProductoTerminado) CafeDarkBrown else SoftGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (articulo.codigo.isNotBlank()) {
                            Text(
                                text = articulo.codigo,
                                fontSize = 11.sp,
                                color = SoftGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = articulo.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Categoría: ${articulo.categoria}",
                        fontSize = 11.sp,
                        color = SoftGray
                    )
                }

                // Badge de Stock
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBajoStock) SoftRed.copy(alpha = 0.18f) else SoftGreen.copy(alpha = 0.18f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${formatQty(articulo.stockActual)} ${articulo.unidadMedida}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isBajoStock) SoftRed else SoftGreen
                        )
                        Text(
                            text = if (isBajoStock) "⚠️ Bajo Stock" else (if (isProductoTerminado) "En Vitrina" else "En Almacén"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBajoStock) SoftRed else SoftGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resumen de Receta en Producto Terminado
            if (isProductoTerminado) {
                Surface(
                    color = CafeBrown.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 Receta estipulada (${ingredientesReceta.size} ingredientes):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CafeDarkBrown
                            )
                            Text(
                                text = "Modificar",
                                fontSize = 11.sp,
                                color = CafeBrown,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onEditarReceta() }
                            )
                        }
                        if (ingredientesReceta.isEmpty()) {
                            Text(
                                text = "⚠️ Sin receta asignada. Toca 'Modificar' para vincular materias primas a descontar.",
                                fontSize = 10.sp,
                                color = SoftRed
                            )
                        } else {
                            val resumen = ingredientesReceta.joinToString(", ") {
                                "${formatQty(it.cantidadPorUnidad)} ${it.unidadMedida} ${it.materiaPrimaNombre}"
                            }
                            Text(
                                text = "Por 1 ${articulo.unidadMedida}: $resumen",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Datos financieros del artículo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val montoPrincipal = if (isProductoTerminado) articulo.precioVenta else articulo.costoUnitario
                    Text(if (isProductoTerminado) "Precio Venta" else "Costo Unit.", fontSize = 10.sp, color = SoftGray)
                    Text(
                        FormatUtils.formatCurrency(montoPrincipal, monedaSimbolo),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProductoTerminado) SoftGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        FormatUtils.formatBs(montoPrincipal, tasaCambioBs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CafeDarkBrown
                    )
                }
                Column {
                    Text("Stock Mínimo", fontSize = 10.sp, color = SoftGray)
                    Text(
                        "${formatQty(articulo.stockMinimo)} ${articulo.unidadMedida}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val valorAlmacen = articulo.stockActual * articulo.costoUnitario
                    Text("Valor en Almacén", fontSize = 10.sp, color = SoftGray)
                    Text(
                        FormatUtils.formatCurrency(valorAlmacen, monedaSimbolo),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CafeDarkBrown
                    )
                    Text(
                        FormatUtils.formatBs(valorAlmacen, tasaCambioBs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGray
                    )
                }
            }

            if (articulo.notas.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Nota: ${articulo.notas}",
                    fontSize = 11.sp,
                    color = SoftGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProductoTerminado) {
                    // Botón para Elaborar / Pasar a Vitrina con rebaja de insumos
                    Button(
                        onClick = onElaborarParaVitrina,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CafeDarkBrown,
                            contentColor = SmoothBeige
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("elaborar_producto_button_${articulo.id}")
                    ) {
                        Text("🥐 Elaborar para Vitrina", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Botón para Movimiento de Materia Prima
                    Button(
                        onClick = onAjustarStock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CafeDarkBrown,
                            contentColor = SmoothBeige
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("movimiento_stock_button_${articulo.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajustar Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isProductoTerminado) {
                        IconButton(
                            onClick = onEditarReceta,
                            modifier = Modifier.size(34.dp).testTag("recipe_button_${articulo.id}")
                        ) {
                            Text("📜", fontSize = 16.sp)
                        }
                    }
                    IconButton(
                        onClick = onAjustarStock,
                        modifier = Modifier.size(34.dp).testTag("quick_adjust_button_${articulo.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ajustar", tint = CafeBrown, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier.size(34.dp).testTag("edit_articulo_button_${articulo.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CafeBrown, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onBorrar,
                        modifier = Modifier.size(34.dp).testTag("delete_articulo_button_${articulo.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = SoftRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MovimientoInventarioCard(
    movimiento: MovimientoInventario,
    onBorrar: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val fechaStr = remember(movimiento.fecha) { sdf.format(Date(movimiento.fecha)) }

    val (badgeColor, badgeIcon, sign) = when (movimiento.tipo) {
        "ENTRADA" -> Triple(SoftGreen, "⬇️ Entrada", "+")
        "PRODUCCION" -> Triple(CafeDarkBrown, "🥐 Producción", "+")
        "SALIDA" -> Triple(SoftRed, "⬆️ Salida", "-")
        else -> Triple(CafeDarkBrown, "🔄 Ajuste", "≈")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
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
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeIcon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fechaStr,
                        fontSize = 11.sp,
                        color = SoftGray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = movimiento.articuloNombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Motivo: ${movimiento.motivo}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Stock: ${formatQty(movimiento.stockAnterior)} ➔ ${formatQty(movimiento.stockNuevo)}",
                    fontSize = 11.sp,
                    color = SoftGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${formatQty(movimiento.cantidad)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor
                )
                IconButton(
                    onClick = onBorrar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = SoftGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// DIALOG: Agregar o Editar Artículo
@Composable
fun ArticuloFormDialog(
    title: String,
    initialArticulo: ArticuloInventario? = null,
    initialTipo: String = "MATERIA_PRIMA",
    monedaSimbolo: String,
    onDismiss: () -> Unit,
    onConfirm: (
        codigo: String,
        nombre: String,
        tipo: String,
        categoria: String,
        unidadMedida: String,
        stockInicial: Double,
        stockMinimo: Double,
        costoUnitario: Double,
        precioVenta: Double,
        notas: String
    ) -> Unit
) {
    var tipoInventario by remember { mutableStateOf(initialArticulo?.tipoInventario ?: initialTipo) }
    var codigo by remember { mutableStateOf(initialArticulo?.codigo ?: "") }
    var nombre by remember { mutableStateOf(initialArticulo?.nombre ?: "") }
    var categoria by remember {
        mutableStateOf(
            initialArticulo?.categoria ?: if (tipoInventario == "MATERIA_PRIMA") "Materia Prima" else "Empanadas y Frituras"
        )
    }
    var unidadMedida by remember {
        mutableStateOf(
            initialArticulo?.unidadMedida ?: if (tipoInventario == "MATERIA_PRIMA") "Kg" else "Unidad"
        )
    }
    var stockStr by remember { mutableStateOf(initialArticulo?.stockActual?.let { formatQty(it) } ?: "0") }
    var stockMinStr by remember { mutableStateOf(initialArticulo?.stockMinimo?.let { formatQty(it) } ?: "5") }
    var costoStr by remember { mutableStateOf(initialArticulo?.costoUnitario?.let { if (it > 0) it.toString() else "" } ?: "") }
    var precioVentaStr by remember { mutableStateOf(initialArticulo?.precioVenta?.let { if (it > 0) it.toString() else "" } ?: "") }
    var notas by remember { mutableStateOf(initialArticulo?.notas ?: "") }

    val unidades = listOf("Kg", "Litro", "Unidad", "Caja", "Paquete", "Gramo", "Porción", "Taza")
    val categoriasMP = listOf("Materia Prima", "Harinas y Granos", "Carnes y Proteínas", "Lácteos", "Bebidas", "Empaques")
    val categoriasPT = listOf("Empanadas y Frituras", "Arepas y Desayunos", "Cafetería", "Bollitos", "Almuerzos", "Panadería")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Selector de Tipo de Inventario (Materia Prima vs Producto Terminado)
                item {
                    Text("Clasificación de Inventario:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tipoInventario = "MATERIA_PRIMA"
                                if (categoria in categoriasPT) categoria = "Materia Prima"
                                if (unidadMedida == "Unidad") unidadMedida = "Kg"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (tipoInventario == "MATERIA_PRIMA") CafeDarkBrown.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (tipoInventario == "MATERIA_PRIMA") CafeDarkBrown else SoftGray
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (tipoInventario == "MATERIA_PRIMA") CafeDarkBrown else SoftGray.copy(alpha = 0.4f))
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌾 Materia Prima", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                tipoInventario = "PRODUCTO_TERMINADO"
                                if (categoria in categoriasMP) categoria = "Empanadas y Frituras"
                                unidadMedida = "Unidad"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (tipoInventario == "PRODUCTO_TERMINADO") CafeDarkBrown.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (tipoInventario == "PRODUCTO_TERMINADO") CafeDarkBrown else SoftGray
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (tipoInventario == "PRODUCTO_TERMINADO") CafeDarkBrown else SoftGray.copy(alpha = 0.4f))
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🥐 Prod. Terminado", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del Artículo *") },
                        placeholder = {
                            Text(if (tipoInventario == "MATERIA_PRIMA") "Ej: Harina de Maíz, Carne Molida..." else "Ej: Empanada de Carne, Arepa Reina...")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("articulo_input_nombre")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = it },
                            label = { Text("Código / SKU") },
                            placeholder = { Text(if (tipoInventario == "MATERIA_PRIMA") "MP-01" else "PT-01") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("articulo_input_codigo")
                        )

                        OutlinedTextField(
                            value = unidadMedida,
                            onValueChange = { unidadMedida = it },
                            label = { Text("Unidad de Medida") },
                            placeholder = { Text("Kg, Litro, Unidad...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("articulo_input_unidad")
                        )
                    }
                }

                item {
                    Text("Sugerencias de unidad:", fontSize = 11.sp, color = SoftGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        unidades.take(4).forEach { u ->
                            SuggestionChip(
                                onClick = { unidadMedida = u },
                                label = { Text(u, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = { categoria = it },
                        label = { Text("Categoría") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("articulo_input_categoria")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sugeridas = if (tipoInventario == "MATERIA_PRIMA") categoriasMP else categoriasPT
                        sugeridas.take(3).forEach { cat ->
                            SuggestionChip(
                                onClick = { categoria = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text(if (tipoInventario == "MATERIA_PRIMA") "Stock en Almacén *" else "Stock en Vitrina *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("articulo_input_stock")
                        )

                        OutlinedTextField(
                            value = stockMinStr,
                            onValueChange = { stockMinStr = it },
                            label = { Text("Stock Mínimo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("articulo_input_stock_min")
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = costoStr,
                            onValueChange = { costoStr = it },
                            label = { Text("Costo Unit. ($monedaSimbolo)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("articulo_input_costo")
                        )

                        OutlinedTextField(
                            value = precioVentaStr,
                            onValueChange = { precioVentaStr = it },
                            label = { Text("Precio Vta. ($monedaSimbolo)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("articulo_input_precio_venta")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text("Notas / Ubicación") },
                        placeholder = { Text("Ej: Vitrina caliente, estante 2, nevera...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        val stockVal = stockStr.toDoubleOrNull() ?: 0.0
                        val stockMinVal = stockMinStr.toDoubleOrNull() ?: 5.0
                        val costoVal = costoStr.toDoubleOrNull() ?: 0.0
                        val pVentaVal = precioVentaStr.toDoubleOrNull() ?: 0.0
                        onConfirm(codigo, nombre, tipoInventario, categoria, unidadMedida, stockVal, stockMinVal, costoVal, pVentaVal, notas)
                    }
                },
                enabled = nombre.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown, contentColor = SmoothBeige),
                modifier = Modifier.testTag("save_articulo_dialog_confirm")
            ) {
                Text("Guardar Artículo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIALOG: Movimiento Directo de Stock
@Composable
fun MovimientoStockDialog(
    articulo: ArticuloInventario,
    onDismiss: () -> Unit,
    onConfirm: (tipo: String, cantidad: Double, motivo: String) -> Unit
) {
    var tipoMovimiento by remember { mutableStateOf("ENTRADA") }
    var cantidadStr by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }

    val stockActual = articulo.stockActual
    val cant = cantidadStr.toDoubleOrNull() ?: 0.0

    val nuevoStockEstimado = when (tipoMovimiento) {
        "ENTRADA" -> stockActual + cant
        "SALIDA" -> (stockActual - cant).coerceAtLeast(0.0)
        "AJUSTE" -> cant
        else -> stockActual
    }

    val motivosSugeridos = when (tipoMovimiento) {
        "ENTRADA" -> listOf("Compra a proveedor", "Devolución", "Carga inicial")
        "SALIDA" -> listOf("Consumo interno", "Merma / Dañado", "Vencimiento")
        else -> listOf("Ajuste físico de inventario", "Conteo mensual", "Corrección")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Movimiento de Stock", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(articulo.nombre, fontSize = 13.sp, color = CafeBrown)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stock Actual:", fontSize = 13.sp, color = SoftGray)
                        Text(
                            "${formatQty(stockActual)} ${articulo.unidadMedida}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text("Tipo de Operación:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ops = listOf(
                        Triple("ENTRADA", "⬇️ Entrada", SoftGreen),
                        Triple("SALIDA", "⬆️ Salida", SoftRed),
                        Triple("AJUSTE", "🔄 Ajuste", CafeDarkBrown)
                    )
                    ops.forEach { (tipo, label, color) ->
                        val isSelected = tipoMovimiento == tipo
                        OutlinedButton(
                            onClick = {
                                tipoMovimiento = tipo
                                motivo = ""
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) color else SoftGray.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                OutlinedTextField(
                    value = cantidadStr,
                    onValueChange = { cantidadStr = it },
                    label = {
                        Text(
                            if (tipoMovimiento == "AJUSTE") "Nuevo Stock Real Total (${articulo.unidadMedida})"
                            else "Cantidad a ${if (tipoMovimiento == "ENTRADA") "Ingresar" else "Retirar"} (${articulo.unidadMedida}) *"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("movimiento_cantidad_input")
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo / Explicación *") },
                    placeholder = { Text("Ej: Compra factura #124, merma...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("movimiento_motivo_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    motivosSugeridos.take(2).forEach { s ->
                        SuggestionChip(
                            onClick = { motivo = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }

                Surface(
                    color = CafeBrown.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nuevo stock resultante:", fontSize = 12.sp)
                        Text(
                            "${formatQty(nuevoStockEstimado)} ${articulo.unidadMedida}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = CafeDarkBrown
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cant > 0 || (tipoMovimiento == "AJUSTE" && cantidadStr.isNotBlank())) {
                        onConfirm(tipoMovimiento, cant, motivo.ifBlank { "Movimiento de stock" })
                    }
                },
                enabled = cant > 0 || (tipoMovimiento == "AJUSTE" && cantidadStr.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown, contentColor = SmoothBeige),
                modifier = Modifier.testTag("confirm_movimiento_button")
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// DIALOG: Configuración de Receta para Producto Terminado
@Composable
fun RecetaConfigDialog(
    productoTerminado: ArticuloInventario,
    materiasPrimasDisponibles: List<ArticuloInventario>,
    ingredientesActuales: List<RecetaIngrediente>,
    onDismiss: () -> Unit,
    onSave: (List<RecetaIngrediente>) -> Unit
) {
    val ingredientesList = remember { mutableStateListOf<RecetaIngrediente>().apply { addAll(ingredientesActuales) } }
    var selectedMateriaPrimaId by remember { mutableStateOf(materiasPrimasDisponibles.firstOrNull()?.id ?: 0) }
    var cantidadPorUnidadStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("📜 Receta Estipulada", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Para: ${productoTerminado.nombre} (por 1 ${productoTerminado.unidadMedida})",
                    fontSize = 13.sp,
                    color = CafeBrown
                )
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
                        "Define cuánta materia prima se consume al preparar 1 unidad de este producto para la vitrina.",
                        fontSize = 12.sp,
                        color = SoftGray
                    )
                }

                // Lista de ingredientes actuales
                if (ingredientesList.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "No hay materias primas agregadas a esta receta.",
                                fontSize = 12.sp,
                                color = SoftGray,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    items(ingredientesList) { ing ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ing.materiaPrimaNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "${formatQty(ing.cantidadPorUnidad)} ${ing.unidadMedida} por cada ${productoTerminado.unidadMedida}",
                                        fontSize = 11.sp,
                                        color = SoftGray
                                    )
                                }
                                IconButton(onClick = { ingredientesList.remove(ing) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = SoftRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Formulario para agregar ingrediente
                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Agregar Insumo a la Receta:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    if (materiasPrimasDisponibles.isEmpty()) {
                        Text(
                            "Primero debes registrar artículos en el inventario de Materia Prima.",
                            fontSize = 12.sp,
                            color = SoftRed
                        )
                    } else {
                        // Selección de materia prima
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Selecciona Insumo:", fontSize = 11.sp, color = SoftGray)
                            materiasPrimasDisponibles.take(5).forEach { mp ->
                                val isSelected = selectedMateriaPrimaId == mp.id
                                Surface(
                                    color = if (isSelected) CafeBrown.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedMateriaPrimaId = mp.id }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) CafeBrown else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(mp.nombre, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        Text("${mp.unidadMedida}", fontSize = 11.sp, color = SoftGray)
                                    }
                                }
                            }

                            val mpSeleccionada = materiasPrimasDisponibles.find { it.id == selectedMateriaPrimaId }
                            OutlinedTextField(
                                value = cantidadPorUnidadStr,
                                onValueChange = { cantidadPorUnidadStr = it },
                                label = { Text("Cantidad por 1 ${productoTerminado.unidadMedida} (${mpSeleccionada?.unidadMedida ?: ""})") },
                                placeholder = { Text("Ej: 0.08") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("receta_cantidad_input")
                            )

                            Button(
                                onClick = {
                                    val cant = cantidadPorUnidadStr.toDoubleOrNull() ?: 0.0
                                    val mp = mpSeleccionada
                                    if (cant > 0 && mp != null) {
                                        // Evitar duplicados
                                        ingredientesList.removeAll { it.materiaPrimaId == mp.id }
                                        ingredientesList.add(
                                            RecetaIngrediente(
                                                productoTerminadoId = productoTerminado.id,
                                                materiaPrimaId = mp.id,
                                                materiaPrimaNombre = mp.nombre,
                                                cantidadPorUnidad = cant,
                                                unidadMedida = mp.unidadMedida
                                            )
                                        )
                                        cantidadPorUnidadStr = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CafeBrown),
                                modifier = Modifier.fillMaxWidth().testTag("agregar_ingrediente_button")
                            ) {
                                Text("+ Agregar a la Receta", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ingredientesList.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown, contentColor = SmoothBeige),
                modifier = Modifier.testTag("guardar_receta_button")
            ) {
                Text("Guardar Receta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

// DIALOG: Elaborar Producto Terminado para Vitrina (Descontar de Materia Prima)
@Composable
fun ElaborarProductoDialog(
    productoTerminado: ArticuloInventario,
    ingredientesReceta: List<RecetaIngrediente>,
    materiasPrimas: List<ArticuloInventario>,
    onDismiss: () -> Unit,
    onConfirmElaborar: (cantidad: Double, motivo: String) -> Unit
) {
    var cantidadElaborarStr by remember { mutableStateOf("20") }
    var motivo by remember { mutableStateOf("Elaboración matutina para vitrina de ventas") }

    val cantElaborar = cantidadElaborarStr.toDoubleOrNull() ?: 0.0

    // Cálculo de stock resultante y deducción de materias primas
    val insumosCalculados = remember(cantElaborar, ingredientesReceta, materiasPrimas) {
        ingredientesReceta.map { ing ->
            val mp = materiasPrimas.find { it.id == ing.materiaPrimaId }
            val cantRebajar = ing.cantidadPorUnidad * cantElaborar
            val stockActualMP = mp?.stockActual ?: 0.0
            val stockResultanteMP = (stockActualMP - cantRebajar).coerceAtLeast(0.0)
            val alcanzaStock = stockActualMP >= cantRebajar
            InsumoDeduccion(
                materiaPrimaNombre = ing.materiaPrimaNombre,
                unidadMedida = ing.unidadMedida,
                cantidadRebajar = cantRebajar,
                stockActual = stockActualMP,
                stockResultante = stockResultanteMP,
                alcanzaStock = alcanzaStock
            )
        }
    }

    val algunInsumoInsuficiente = insumosCalculados.any { !it.alcanzaStock }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("🥐 Elaborar para Vitrina", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    productoTerminado.nombre,
                    fontSize = 14.sp,
                    color = CafeBrown,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Ingresa la cantidad que pasó a la vitrina. El sistema aumentará el stock de este producto terminado y descontará automáticamente la materia prima correspondiente.",
                        fontSize = 12.sp,
                        color = SoftGray,
                        lineHeight = 16.sp
                    )
                }

                item {
                    OutlinedTextField(
                        value = cantidadElaborarStr,
                        onValueChange = { cantidadElaborarStr = it },
                        label = { Text("Cantidad a Pasar a Vitrina (${productoTerminado.unidadMedida}) *") },
                        placeholder = { Text("Ej: 20") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("elaborar_cantidad_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo / Turno") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Previsualización de Materia Prima a descontar
                item {
                    Text("Rebaja Automática de Materia Prima:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    if (ingredientesReceta.isEmpty()) {
                        Surface(
                            color = SoftRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "⚠️ Este producto no tiene receta estipulada. Se sumará a la vitrina pero no se descontarán insumos de materia prima.",
                                fontSize = 11.sp,
                                color = SoftRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            insumosCalculados.forEach { ins ->
                                Surface(
                                    color = if (ins.alcanzaStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else SoftRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ins.materiaPrimaNombre,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "Stock: ${formatQty(ins.stockActual)} ➔ ${formatQty(ins.stockResultante)} ${ins.unidadMedida}",
                                                fontSize = 11.sp,
                                                color = if (ins.alcanzaStock) SoftGray else SoftRed
                                            )
                                        }
                                        Text(
                                            "-${formatQty(ins.cantidadRebajar)} ${ins.unidadMedida}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (ins.alcanzaStock) SoftRed else SoftRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Resultado final en vitrina
                item {
                    Surface(
                        color = CafeBrown.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stock final en vitrina:", fontSize = 13.sp)
                            Text(
                                "${formatQty(productoTerminado.stockActual + cantElaborar)} ${productoTerminado.unidadMedida}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CafeDarkBrown
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cantElaborar > 0) {
                        onConfirmElaborar(cantElaborar, motivo.ifBlank { "Elaboración para vitrina" })
                    }
                },
                enabled = cantElaborar > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown, contentColor = SmoothBeige),
                modifier = Modifier.testTag("confirm_elaborar_button")
            ) {
                Text("Confirmar y Rebajar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class InsumoDeduccion(
    val materiaPrimaNombre: String,
    val unidadMedida: String,
    val cantidadRebajar: Double,
    val stockActual: Double,
    val stockResultante: Double,
    val alcanzaStock: Boolean
)

private fun formatQty(num: Double): String {
    return if (num % 1.0 == 0.0) {
        num.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", num)
    }
}
