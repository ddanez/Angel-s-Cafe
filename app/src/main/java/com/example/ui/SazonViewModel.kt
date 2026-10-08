package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ClienteConSaldo
import com.example.data.repository.ProveedorConSaldo
import com.example.data.repository.SazonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SazonViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SazonRepository(
        clienteDao = db.clienteDao(),
        platoDao = db.platoDao(),
        transaccionDao = db.transaccionDao(),
        proveedorDao = db.proveedorDao(),
        compraDao = db.compraDao(),
        pagoProveedorDao = db.pagoProveedorDao(),
        configuracionDao = db.configuracionDao(),
        inventarioDao = db.inventarioDao(),
        movimientoInventarioDao = db.movimientoInventarioDao(),
        recetaIngredienteDao = db.recetaIngredienteDao()
    )

    val clientesConSaldo: StateFlow<List<ClienteConSaldo>> = repository.clientesConSaldo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val proveedoresConSaldo: StateFlow<List<ProveedorConSaldo>> = repository.proveedoresConSaldo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val platos: StateFlow<List<Plato>> = repository.platos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transacciones: StateFlow<List<Transaccion>> = repository.transacciones
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val compras: StateFlow<List<Compra>> = repository.compras
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pagosProveedor: StateFlow<List<PagoProveedor>> = repository.pagosProveedor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val configuracion: StateFlow<ConfiguracionComercio> = repository.configuracion
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConfiguracionComercio()
        )

    val articulosInventario: StateFlow<List<ArticuloInventario>> = repository.articulosInventario
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val movimientosInventario: StateFlow<List<MovimientoInventario>> = repository.movimientosInventario
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recetasIngredientes: StateFlow<List<RecetaIngrediente>> = repository.recetasIngredientes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )



    // State for selected client in client detail
    private val _selectedClienteId = MutableStateFlow<Int?>(null)
    val selectedClienteId: StateFlow<Int?> = _selectedClienteId.asStateFlow()

    val transaccionesDelClienteSeleccionado: StateFlow<List<Transaccion>> = combine(
        _selectedClienteId,
        repository.transacciones
    ) { id, allTx ->
        if (id == null) {
            emptyList()
        } else {
            allTx.filter { it.clienteId == id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selection of food item cart state for POS
    private val _carrito = MutableStateFlow<Map<Plato, Int>>(emptyMap())
    val carrito: StateFlow<Map<Plato, Int>> = _carrito.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // --- CLIENTS ---
    fun selectCliente(id: Int?) {
        _selectedClienteId.value = id
    }

    fun agregarCliente(nombre: String, telefono: String, direccion: String, notas: String = "") {
        viewModelScope.launch {
            repository.addCliente(
                Cliente(nombre = nombre.trim(), telefono = telefono.trim(), direccion = direccion.trim(), notas = notas.trim())
            )
        }
    }

    fun editarCliente(cliente: Cliente) {
        viewModelScope.launch {
            repository.updateCliente(cliente)
        }
    }

    fun borrarCliente(cliente: Cliente) {
        viewModelScope.launch {
            if (_selectedClienteId.value == cliente.id) {
                _selectedClienteId.value = null
            }
            repository.deleteCliente(cliente)
        }
    }

    // --- CART & SALES ---
    fun agregarAlCarrito(plato: Plato) {
        val actual = _carrito.value.toMutableMap()
        actual[plato] = (actual[plato] ?: 0) + 1
        _carrito.value = actual
    }

    fun removerDelCarrito(plato: Plato) {
        val actual = _carrito.value.toMutableMap()
        val count = actual[plato] ?: 0
        if (count <= 1) {
            actual.remove(plato)
        } else {
            actual[plato] = count - 1
        }
        _carrito.value = actual
    }

    fun limpiarCarrito() {
        _carrito.value = emptyMap()
    }

    fun registrarVentaCarrito(clienteId: Int, esCredito: Boolean, notaOpcional: String = "") {
        val itemsEnCarrito = _carrito.value
        if (itemsEnCarrito.isEmpty()) return

        val total = itemsEnCarrito.entries.sumOf { it.key.precio * it.value }
        val resumenItems = itemsEnCarrito.entries.joinToString(", ") { "${it.value}x ${it.key.nombre}" }
        val detalle = if (notaOpcional.isNotBlank()) "$resumenItems ($notaOpcional)" else resumenItems

        viewModelScope.launch {
            repository.addTransaccion(
                Transaccion(
                    clienteId = clienteId,
                    fecha = System.currentTimeMillis(),
                    detalle = detalle,
                    montoTotal = total,
                    tipo = if (esCredito) "COMPRA" else "VENTA_CONTADO"
                )
            )
            limpiarCarrito()
        }
    }

    fun registrarVentaO_AbonoManual(clienteId: Int, monto: Double, detalle: String, tipo: String) {
        viewModelScope.launch {
            repository.addTransaccion(
                Transaccion(
                    clienteId = clienteId,
                    fecha = System.currentTimeMillis(),
                    detalle = detalle,
                    montoTotal = monto,
                    tipo = tipo
                )
            )
        }
    }

    fun borrarTransaccion(transaccion: Transaccion) {
        viewModelScope.launch {
            repository.deleteTransaccion(transaccion)
        }
    }

    // --- PROVEEDORES ---
    fun agregarProveedor(nombre: String, empresa: String, telefono: String, direccion: String, notas: String = "") {
        viewModelScope.launch {
            repository.addProveedor(
                Proveedor(
                    nombre = nombre.trim(),
                    empresa = empresa.trim(),
                    telefono = telefono.trim(),
                    direccion = direccion.trim(),
                    notas = notas.trim()
                )
            )
        }
    }

    fun editarProveedor(proveedor: Proveedor) {
        viewModelScope.launch {
            repository.updateProveedor(proveedor)
        }
    }

    fun borrarProveedor(proveedor: Proveedor) {
        viewModelScope.launch {
            repository.deleteProveedor(proveedor)
        }
    }

    // --- COMPRAS ---
    fun registrarCompra(
        proveedorId: Int,
        proveedorNombre: String,
        detalle: String,
        monto: Double,
        condicion: String
    ) {
        viewModelScope.launch {
            repository.addCompra(
                Compra(
                    proveedorId = proveedorId,
                    proveedorNombre = proveedorNombre,
                    fecha = System.currentTimeMillis(),
                    detalle = detalle,
                    montoTotal = monto,
                    condicion = condicion
                )
            )
        }
    }

    fun borrarCompra(compra: Compra) {
        viewModelScope.launch {
            repository.deleteCompra(compra)
        }
    }

    // --- CXP (PAGOS A PROVEEDORES) ---
    fun registrarPagoProveedor(proveedorId: Int, monto: Double, detalle: String) {
        viewModelScope.launch {
            repository.addPagoProveedor(
                PagoProveedor(
                    proveedorId = proveedorId,
                    fecha = System.currentTimeMillis(),
                    detalle = detalle,
                    monto = monto
                )
            )
        }
    }

    fun borrarPagoProveedor(pago: PagoProveedor) {
        viewModelScope.launch {
            repository.deletePagoProveedor(pago)
        }
    }

    // --- PLATOS / MENU ---
    fun agregarPlato(nombre: String, precio: Double, descripcion: String, categoria: String = "General") {
        viewModelScope.launch {
            repository.addPlato(
                Plato(
                    nombre = nombre.trim(),
                    precio = precio,
                    descripcion = descripcion.trim(),
                    categoria = categoria.trim()
                )
            )
        }
    }

    fun editarPlato(plato: Plato) {
        viewModelScope.launch {
            repository.updatePlato(plato)
        }
    }

    fun borrarPlato(plato: Plato) {
        viewModelScope.launch {
            repository.deletePlato(plato)
        }
    }

    // --- CONFIGURACION ---
    fun guardarConfiguracion(config: ConfiguracionComercio) {
        viewModelScope.launch {
            repository.saveConfiguracion(config)
        }
    }

    // --- INVENTARIO ---
    fun agregarArticuloInventario(
        codigo: String,
        nombre: String,
        tipoInventario: String = "MATERIA_PRIMA",
        categoria: String,
        unidadMedida: String,
        stockInicial: Double,
        stockMinimo: Double,
        costoUnitario: Double,
        precioVenta: Double,
        notas: String
    ) {
        viewModelScope.launch {
            val art = ArticuloInventario(
                codigo = codigo.trim(),
                nombre = nombre.trim(),
                tipoInventario = tipoInventario,
                categoria = categoria.trim(),
                unidadMedida = unidadMedida.trim(),
                stockActual = stockInicial,
                stockMinimo = stockMinimo,
                costoUnitario = costoUnitario,
                precioVenta = precioVenta,
                notas = notas.trim()
            )
            val newId = repository.addArticuloInventario(art).toInt()
            if (stockInicial > 0) {
                repository.registrarMovimientoInventario(
                    articuloId = newId,
                    articuloNombre = art.nombre,
                    tipo = if (tipoInventario == "PRODUCTO_TERMINADO") "PRODUCCION" else "ENTRADA",
                    cantidad = stockInicial,
                    stockAnterior = 0.0,
                    stockNuevo = stockInicial,
                    motivo = "Inventario inicial al crear artículo ($tipoInventario)"
                )
            }
        }
    }

    fun editarArticuloInventario(articulo: ArticuloInventario) {
        viewModelScope.launch {
            repository.updateArticuloInventario(articulo)
        }
    }

    fun borrarArticuloInventario(articulo: ArticuloInventario) {
        viewModelScope.launch {
            repository.deleteArticuloInventario(articulo)
        }
    }

    fun registrarAjusteO_MovimientoStock(
        articulo: ArticuloInventario,
        tipo: String, // "ENTRADA", "SALIDA", "AJUSTE"
        cantidad: Double,
        motivo: String
    ) {
        viewModelScope.launch {
            val stockAnterior = articulo.stockActual
            val stockNuevo = when (tipo) {
                "ENTRADA" -> stockAnterior + cantidad
                "SALIDA" -> (stockAnterior - cantidad).coerceAtLeast(0.0)
                "AJUSTE" -> cantidad // El usuario define el nuevo stock real
                else -> stockAnterior
            }
            val cantAfectada = if (tipo == "AJUSTE") kotlin.math.abs(stockNuevo - stockAnterior) else cantidad
            val artActualizado = articulo.copy(stockActual = stockNuevo)
            repository.updateArticuloInventario(artActualizado)
            repository.registrarMovimientoInventario(
                articuloId = articulo.id,
                articuloNombre = articulo.nombre,
                tipo = tipo,
                cantidad = cantAfectada,
                stockAnterior = stockAnterior,
                stockNuevo = stockNuevo,
                motivo = motivo.trim().ifEmpty { "Ajuste de inventario manual" }
            )
        }
    }

    fun borrarMovimientoInventario(movimiento: MovimientoInventario) {
        viewModelScope.launch {
            repository.deleteMovimientoInventario(movimiento)
        }
    }

    // --- RECETAS Y ELABORACIÓN DE PRODUCTO TERMINADO ---
    fun guardarRecetaIngredientes(productoId: Int, ingredientes: List<RecetaIngrediente>) {
        viewModelScope.launch {
            repository.guardarIngredientesReceta(productoId, ingredientes)
        }
    }

    fun elaborarProductoParaVitrina(
        productoTerminadoId: Int,
        cantidad: Double,
        motivo: String = "Elaboración para vitrina de ventas",
        onResultado: (mensajeError: String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.elaborarProductoTerminado(
                productoTerminadoId = productoTerminadoId,
                cantidadElaborada = cantidad,
                motivoProduccion = motivo
            )
            onResultado(res)
        }
    }

    // --- HARD RESET ---
    fun ejecutarHardReset(reinicializarDatosEjemplo: Boolean, onCompletado: () -> Unit) {
        viewModelScope.launch {
            _selectedClienteId.value = null
            limpiarCarrito()
            repository.hardResetDatabase(reinicializarDatosEjemplo)
            onCompletado()
        }
    }

    // --- LICENCIAMIENTO ---
    fun activarLicencia(clave: String, titular: String, onResultado: (Boolean, String) -> Unit) {
        val claveLimpia = clave.trim().uppercase()
        val titularLimpio = titular.trim()

        if (claveLimpia.length < 8) {
            onResultado(false, "La clave de licencia debe tener al menos 8 caracteres.")
            return
        }

        viewModelScope.launch {
            val configActual = configuracion.value
            val configActualizada = configActual.copy(
                claveLicencia = claveLimpia,
                titularLicencia = titularLimpio.ifBlank { configActual.titularLicencia },
                estadoLicencia = "ACTIVA",
                fechaActivacion = System.currentTimeMillis()
            )
            repository.saveConfiguracion(configActualizada)
            onResultado(true, "¡Licencia activada con éxito para $titularLimpio!")
        }
    }
}



