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
            // Comprobación de actualización diaria de tasa si está habilitada
            verificarYActualizarTasaDiaria()
        }
    }

    // --- TASA DE CAMBIO USD / BOLÍVARES (BS) ---
    private val _actualizandoTasa = MutableStateFlow(false)
    val actualizandoTasa: StateFlow<Boolean> = _actualizandoTasa.asStateFlow()

    private val _mensajeTasa = MutableStateFlow<String?>(null)
    val mensajeTasa: StateFlow<String?> = _mensajeTasa.asStateFlow()

    fun actualizarTasaCambioManual(nuevaTasa: Double) {
        if (nuevaTasa <= 0) return
        viewModelScope.launch {
            val cfg = configuracion.value
            val actualizada = cfg.copy(
                tasaCambioBs = nuevaTasa,
                fechaActualizacionTasa = System.currentTimeMillis()
            )
            repository.saveConfiguracion(actualizada)
            _mensajeTasa.value = "Tasa fijada manualmente a Bs. ${String.format(java.util.Locale.US, "%.2f", nuevaTasa)}"
        }
    }

    fun toggleAutoActualizarTasa(habilitado: Boolean) {
        viewModelScope.launch {
            val cfg = configuracion.value
            repository.saveConfiguracion(cfg.copy(autoActualizarTasa = habilitado))
            if (habilitado) {
                actualizarTasaDesdeInternet(forzar = true)
            }
        }
    }

    fun actualizarTasaDesdeInternet(forzar: Boolean = false, onCompletado: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _actualizandoTasa.value = true
            try {
                val tasaOnline = com.example.data.network.TasaCambioService.obtenerTasaDolarBs()
                if (tasaOnline != null && tasaOnline > 0) {
                    val cfg = configuracion.first()
                    repository.saveConfiguracion(
                        cfg.copy(
                            tasaCambioBs = tasaOnline,
                            fechaActualizacionTasa = System.currentTimeMillis()
                        )
                    )
                    val msg = "Tasa oficial actualizada: Bs. ${String.format(java.util.Locale.US, "%.2f", tasaOnline)}"
                    _mensajeTasa.value = msg
                    onCompletado?.invoke(true, msg)
                } else {
                    val msg = "No se pudo consultar la tasa en línea. Se mantiene Bs. ${String.format(java.util.Locale.US, "%.2f", configuracion.value.tasaCambioBs)}"
                    _mensajeTasa.value = msg
                    onCompletado?.invoke(false, msg)
                }
            } catch (e: Exception) {
                val msg = "Error al actualizar tasa: ${e.message}"
                _mensajeTasa.value = msg
                onCompletado?.invoke(false, msg)
            } finally {
                _actualizandoTasa.value = false
            }
        }
    }

    private fun verificarYActualizarTasaDiaria() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val cfg = configuracion.first()
            if (cfg.autoActualizarTasa) {
                actualizarTasaDesdeInternet(forzar = true)
            }
        }
    }

    fun limpiarMensajeTasa() {
        _mensajeTasa.value = null
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
    /**
     * Activa o modifica la licencia con las opciones:
     * - MENSUAL (30 días)
     * - SEMESTRAL (180 días)
     * - ANUAL (365 días)
     * - VITALICIA (Sin caducidad)
     * Requiere obligatoriamente la clave secreta de autorización (9900).
     */
    fun activarLicenciaPlan(
        clave: String,
        plan: String, // "MENSUAL", "SEMESTRAL", "ANUAL", "VITALICIA"
        titular: String,
        onResultado: (Boolean, String) -> Unit
    ) {
        val claveNormalizada = clave.trim().replace(" ", "")
        val claveValida = claveNormalizada == "9900"

        if (!claveValida) {
            onResultado(false, "Clave de autorización incorrecta. Debe ingresar la clave válida provista por su desarrollador.")
            return
        }

        val titularLimpio = titular.trim().ifBlank { configuracion.value.titularLicencia }
        val ahora = System.currentTimeMillis()
        val dias = when (plan) {
            "MENSUAL" -> 30L
            "SEMESTRAL" -> 180L
            "ANUAL" -> 365L
            else -> 0L // Vitalicia
        }

        val fechaVencimiento = if (dias > 0) ahora + (dias * 24 * 60 * 60 * 1000L) else 0L
        val nombreTipo = when (plan) {
            "MENSUAL" -> "Licencia Comercial Mensual (30 días)"
            "SEMESTRAL" -> "Licencia Comercial Semestral (6 meses)"
            "ANUAL" -> "Licencia Comercial Anual (1 año)"
            else -> "Licencia Comercial Vitalicia (Pro Offline Permanente)"
        }

        viewModelScope.launch {
            val configActual = configuracion.first()
            val configActualizada = configActual.copy(
                claveLicencia = "",
                titularLicencia = titularLimpio,
                planLicencia = plan,
                tipoLicencia = nombreTipo,
                estadoLicencia = "ACTIVA",
                fechaActivacion = ahora,
                fechaVencimientoLicencia = fechaVencimiento,
                fueActivadaConClave = true
            )
            repository.saveConfiguracion(configActualizada)
            onResultado(true, "¡$nombreTipo activada con éxito para $titularLimpio!")
        }
    }

    /**
     * Simula el vencimiento de la licencia para verificar el bloqueo de seguridad.
     */
    fun simularVencimientoLicencia() {
        viewModelScope.launch {
            val configActual = configuracion.first()
            val vencida = configActual.copy(
                fechaVencimientoLicencia = System.currentTimeMillis() - 60000L,
                estadoLicencia = "VENCIDA",
                fueActivadaConClave = false
            )
            repository.saveConfiguracion(vencida)
        }
    }

    // Compatibilidad anterior si se llama sin plan
    fun activarLicencia(clave: String, titular: String, onResultado: (Boolean, String) -> Unit) {
        activarLicenciaPlan(clave, "VITALICIA", titular, onResultado)
    }
}



