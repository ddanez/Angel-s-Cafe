package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class ClienteConSaldo(
    val cliente: Cliente,
    val totalCompras: Double,
    val totalAbonos: Double,
    val saldoPendiente: Double
)

data class ProveedorConSaldo(
    val proveedor: Proveedor,
    val totalComprasCredito: Double,
    val totalPagos: Double,
    val saldoPendienteCXP: Double,
    val totalComprasHistorico: Double
)

class SazonRepository(
    private val clienteDao: ClienteDao,
    private val platoDao: PlatoDao,
    private val transaccionDao: TransaccionDao,
    private val proveedorDao: ProveedorDao,
    private val compraDao: CompraDao,
    private val pagoProveedorDao: PagoProveedorDao,
    private val configuracionDao: ConfiguracionDao,
    private val inventarioDao: InventarioDao,
    private val movimientoInventarioDao: MovimientoInventarioDao,
    private val recetaIngredienteDao: RecetaIngredienteDao
) {
    val clientesConSaldo: Flow<List<ClienteConSaldo>> = combine(
        clienteDao.getAllClientes(),
        transaccionDao.getAllTransacciones()
    ) { listaClientes, listaTransacciones ->
        listaClientes.map { cliente ->
            val txCliente = listaTransacciones.filter { it.clienteId == cliente.id }
            val compras = txCliente.filter { it.tipo == "COMPRA" }.sumOf { it.montoTotal }
            val abonos = txCliente.filter { it.tipo == "ABONO" }.sumOf { it.montoTotal }
            ClienteConSaldo(
                cliente = cliente,
                totalCompras = compras,
                totalAbonos = abonos,
                saldoPendiente = compras - abonos
            )
        }
    }

    val proveedoresConSaldo: Flow<List<ProveedorConSaldo>> = combine(
        proveedorDao.getAllProveedores(),
        compraDao.getAllCompras(),
        pagoProveedorDao.getAllPagosProveedor()
    ) { listaProveedores, listaCompras, listaPagos ->
        listaProveedores.map { prov ->
            val comprasProv = listaCompras.filter { it.proveedorId == prov.id }
            val comprasCredito = comprasProv.filter { it.condicion == "CREDITO" }.sumOf { it.montoTotal }
            val totalHistorico = comprasProv.sumOf { it.montoTotal }
            val pagosRealizados = listaPagos.filter { it.proveedorId == prov.id }.sumOf { it.monto }
            val saldoCXP = (comprasCredito - pagosRealizados).coerceAtLeast(0.0)

            ProveedorConSaldo(
                proveedor = prov,
                totalComprasCredito = comprasCredito,
                totalPagos = pagosRealizados,
                saldoPendienteCXP = saldoCXP,
                totalComprasHistorico = totalHistorico
            )
        }
    }

    val platos: Flow<List<Plato>> = platoDao.getAllPlatos()
    val transacciones: Flow<List<Transaccion>> = transaccionDao.getAllTransacciones()
    val compras: Flow<List<Compra>> = compraDao.getAllCompras()
    val pagosProveedor: Flow<List<PagoProveedor>> = pagoProveedorDao.getAllPagosProveedor()
    val articulosInventario: Flow<List<ArticuloInventario>> = inventarioDao.getAllArticulos()
    val movimientosInventario: Flow<List<MovimientoInventario>> = movimientoInventarioDao.getAllMovimientos()
    val recetasIngredientes: Flow<List<RecetaIngrediente>> = recetaIngredienteDao.getAllRecetas()
    val configuracion: Flow<ConfiguracionComercio> = configuracionDao.getConfiguracion().map {
        it ?: ConfiguracionComercio()
    }

    fun getIngredientesPorProducto(productoId: Int): Flow<List<RecetaIngrediente>> =
        recetaIngredienteDao.getIngredientesByProducto(productoId)

    fun getArticulosPorTipo(tipo: String): Flow<List<ArticuloInventario>> =
        inventarioDao.getArticulosPorTipo(tipo)

    fun getTransaccionesDelCliente(clienteId: Int): Flow<List<Transaccion>> =
        transaccionDao.getTransaccionesByCliente(clienteId)

    suspend fun addCliente(cliente: Cliente): Long = clienteDao.insertCliente(cliente)
    suspend fun updateCliente(cliente: Cliente) = clienteDao.updateCliente(cliente)
    suspend fun deleteCliente(cliente: Cliente) = clienteDao.deleteCliente(cliente)

    suspend fun addPlato(plato: Plato) = platoDao.insertPlato(plato)
    suspend fun updatePlato(plato: Plato) = platoDao.updatePlato(plato)
    suspend fun deletePlato(plato: Plato) = platoDao.deletePlato(plato)

    suspend fun addTransaccion(transaccion: Transaccion): Long =
        transaccionDao.insertTransaccion(transaccion)
    suspend fun deleteTransaccion(transaccion: Transaccion) =
        transaccionDao.deleteTransaccion(transaccion)

    suspend fun addProveedor(proveedor: Proveedor): Long = proveedorDao.insertProveedor(proveedor)
    suspend fun updateProveedor(proveedor: Proveedor) = proveedorDao.updateProveedor(proveedor)
    suspend fun deleteProveedor(proveedor: Proveedor) = proveedorDao.deleteProveedor(proveedor)

    suspend fun addCompra(compra: Compra): Long = compraDao.insertCompra(compra)
    suspend fun deleteCompra(compra: Compra) = compraDao.deleteCompra(compra)

    suspend fun addPagoProveedor(pago: PagoProveedor): Long =
        pagoProveedorDao.insertPagoProveedor(pago)
    suspend fun deletePagoProveedor(pago: PagoProveedor) =
        pagoProveedorDao.deletePagoProveedor(pago)

    suspend fun saveConfiguracion(config: ConfiguracionComercio) =
        configuracionDao.insertOrUpdate(config)

    // --- INVENTARIO ---
    suspend fun addArticuloInventario(articulo: ArticuloInventario): Long =
        inventarioDao.insertArticulo(articulo)

    suspend fun updateArticuloInventario(articulo: ArticuloInventario) =
        inventarioDao.updateArticulo(articulo)

    suspend fun deleteArticuloInventario(articulo: ArticuloInventario) {
        // También limpiar ingredientes de receta si es producto terminado
        recetaIngredienteDao.deleteIngredientesDeProducto(articulo.id)
        inventarioDao.deleteArticulo(articulo)
    }

    suspend fun registrarMovimientoInventario(
        articuloId: Int,
        articuloNombre: String,
        tipo: String,
        cantidad: Double,
        stockAnterior: Double,
        stockNuevo: Double,
        motivo: String
    ) {
        movimientoInventarioDao.insertMovimiento(
            MovimientoInventario(
                articuloId = articuloId,
                articuloNombre = articuloNombre,
                tipo = tipo,
                cantidad = cantidad,
                stockAnterior = stockAnterior,
                stockNuevo = stockNuevo,
                motivo = motivo,
                fecha = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMovimientoInventario(movimiento: MovimientoInventario) =
        movimientoInventarioDao.deleteMovimiento(movimiento)

    // --- RECETAS E INGREDIENTES ---
    suspend fun guardarIngredientesReceta(productoId: Int, ingredientes: List<RecetaIngrediente>) {
        recetaIngredienteDao.deleteIngredientesDeProducto(productoId)
        if (ingredientes.isNotEmpty()) {
            recetaIngredienteDao.insertIngredientes(ingredientes)
        }
    }

    suspend fun getIngredientesDeProductoList(productoId: Int): List<RecetaIngrediente> =
        recetaIngredienteDao.getIngredientesByProductoList(productoId)

    // --- ELABORACIÓN / PRODUCCIÓN (Paso de producto terminado a vitrina y rebaja de materia prima) ---
    suspend fun elaborarProductoTerminado(
        productoTerminadoId: Int,
        cantidadElaborada: Double,
        motivoProduccion: String = "Elaboración para vitrina de ventas"
    ): String? {
        val producto = inventarioDao.getArticuloById(productoTerminadoId) ?: return "Producto terminado no encontrado"
        val ingredientes = recetaIngredienteDao.getIngredientesByProductoList(productoTerminadoId)

        // 1. Aumentar stock del producto terminado
        val stockAnteriorPT = producto.stockActual
        val stockNuevoPT = stockAnteriorPT + cantidadElaborada
        inventarioDao.updateArticulo(producto.copy(stockActual = stockNuevoPT))

        registrarMovimientoInventario(
            articuloId = producto.id,
            articuloNombre = producto.nombre,
            tipo = "PRODUCCION",
            cantidad = cantidadElaborada,
            stockAnterior = stockAnteriorPT,
            stockNuevo = stockNuevoPT,
            motivo = if (motivoProduccion.isNotBlank()) motivoProduccion else "Elaboración para vitrina (${cantidadElaborada.toInt()} ${producto.unidadMedida})"
        )

        // 2. Si tiene receta, descontar cada materia prima proporcionalmente
        for (ing in ingredientes) {
            val mp = inventarioDao.getArticuloById(ing.materiaPrimaId)
            if (mp != null) {
                val cantidadRebajar = ing.cantidadPorUnidad * cantidadElaborada
                val stockAnteriorMP = mp.stockActual
                val stockNuevoMP = (stockAnteriorMP - cantidadRebajar).coerceAtLeast(0.0)
                inventarioDao.updateArticulo(mp.copy(stockActual = stockNuevoMP))

                registrarMovimientoInventario(
                    articuloId = mp.id,
                    articuloNombre = mp.nombre,
                    tipo = "SALIDA",
                    cantidad = cantidadRebajar,
                    stockAnterior = stockAnteriorMP,
                    stockNuevo = stockNuevoMP,
                    motivo = "Rebaja por receta: ${cantidadElaborada.toInt()}x ${producto.nombre}"
                )
            }
        }
        return null // Éxito
    }

    // --- HARD RESET DEL SISTEMA ---
    suspend fun hardResetDatabase(reinicializarDatosEjemplo: Boolean = true) {
        // 1. Borrar todas las tablas
        recetaIngredienteDao.deleteAll()
        movimientoInventarioDao.deleteAll()
        inventarioDao.deleteAll()
        pagoProveedorDao.deleteAll()
        compraDao.deleteAll()
        transaccionDao.deleteAll()
        proveedorDao.deleteAll()
        platoDao.deleteAll()
        clienteDao.deleteAll()
        configuracionDao.deleteAll()

        // 2. Restaurar configuración predeterminada
        val configDefault = ConfiguracionComercio()
        configuracionDao.insertOrUpdate(configDefault)

        // 3. Si se solicita, cargar datos de demostración limpios
        if (reinicializarDatosEjemplo) {
            seedInitialDataIfNeeded()
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        val configActual = configuracionDao.getConfiguracionSync()
        if (configActual == null) {
            val nuevaConfig = ConfiguracionComercio()
            configuracionDao.insertOrUpdate(nuevaConfig)
        } else if (!configActual.fueActivadaConClave && configActual.planLicencia != "DEMO") {
            // Migrar a licencia Demo de 15 días si no fue activada con clave maestra
            val configDemo = configActual.copy(
                planLicencia = "DEMO",
                tipoLicencia = "Licencia de Prueba Demo (15 días)",
                estadoLicencia = "DEMO",
                fechaActivacion = System.currentTimeMillis(),
                fechaVencimientoLicencia = System.currentTimeMillis() + (15L * 24L * 60L * 60L * 1000L),
                fueActivadaConClave = false,
                claveLicencia = ""
            )
            configuracionDao.insertOrUpdate(configDemo)
        }

        if (platoDao.getCount() == 0) {
            val samplePlatos = listOf(
                Plato(nombre = "Café Expreso ☕", precio = 1500.0, descripcion = "Café negro aromático tostado medio"),
                Plato(nombre = "Capuccino Especial 🥛", precio = 2500.0, descripcion = "Espresso con espuma de leche sedosa"),
                Plato(nombre = "Empanada de Carne 🥩", precio = 1800.0, descripcion = "Empanada crocante bien sazonada"),
                Plato(nombre = "Empanada de Queso 🧀", precio = 1600.0, descripcion = "Empanada artesanal con queso derretido"),
                Plato(nombre = "Arepa Reina Pepiada 🥑", precio = 3500.0, descripcion = "Arepa asada rellena de pollo, mayonesa y aguacate"),
                Plato(nombre = "Arepa de Carne Mechada 🥩", precio = 3200.0, descripcion = "Arepa tradicional con carne jugosa"),
                Plato(nombre = "Bollito Aliñado con Mantequilla 🫓", precio = 1200.0, descripcion = "Bollito de masa tierna sazonada"),
                Plato(nombre = "Sándwich Mixto Tostado 🥪", precio = 2400.0, descripcion = "Jamón, queso derretido y mantequilla"),
                Plato(nombre = "Desayuno Americano 🍳", precio = 4500.0, descripcion = "Huevos revueltos, tostadas, tocino y café"),
                Plato(nombre = "Almuerzo Ejecutivo 🍲", precio = 6500.0, descripcion = "Plato principal, guarnición y bebida")
            )
            platoDao.insertPlatos(samplePlatos)
        }

        if (proveedorDao.getCount() == 0) {
            val sampleProveedores = listOf(
                Proveedor(nombre = "Distribuidora Los Andes", empresa = "Café & Granos C.A.", telefono = "+584141234567", direccion = "Zona Industrial Este"),
                Proveedor(nombre = "Lácteos La Pradera", empresa = "Lácteos del Campo", telefono = "+584249876543", direccion = "Av. Principal #45"),
                Proveedor(nombre = "Carnicería y Víveres Central", empresa = "Inversiones Central", telefono = "+584125556677", direccion = "Mercado Municipal Local 12")
            )
            for (p in sampleProveedores) {
                val provId = proveedorDao.insertProveedor(p).toInt()
                if (p.nombre.contains("Distribuidora")) {
                    compraDao.insertCompra(
                        Compra(
                            proveedorId = provId,
                            proveedorNombre = p.nombre,
                            fecha = System.currentTimeMillis() - 86400000L * 4,
                            detalle = "Sacos de harina de maíz + Café en grano + Azúcar",
                            montoTotal = 18500.0,
                            condicion = "CREDITO"
                        )
                    )
                } else if (p.nombre.contains("Lácteos")) {
                    compraDao.insertCompra(
                        Compra(
                            proveedorId = provId,
                            proveedorNombre = p.nombre,
                            fecha = System.currentTimeMillis() - 86400000L * 2,
                            detalle = "Queso blanco paisa (10kg) + Leche pasteurizada",
                            montoTotal = 12000.0,
                            condicion = "CREDITO"
                        )
                    )
                    pagoProveedorDao.insertPagoProveedor(
                        PagoProveedor(
                            proveedorId = provId,
                            fecha = System.currentTimeMillis() - 86400000L,
                            detalle = "Abono transferencia Bancaria",
                            monto = 5000.0
                        )
                    )
                }
            }
        }

        if (inventarioDao.getCount() == 0) {
            // MATERIA PRIMA (Insumos e ingredientes comprados para preparar)
            val mpHarina = ArticuloInventario(
                codigo = "MP-001",
                nombre = "Harina de Maíz Precocida",
                tipoInventario = "MATERIA_PRIMA",
                categoria = "Materia Prima",
                unidadMedida = "Kg",
                stockActual = 30.0,
                stockMinimo = 10.0,
                costoUnitario = 450.0,
                precioVenta = 0.0,
                notas = "Bolsas de 1kg para empanadas, arepas y bollitos"
            )
            val mpCarne = ArticuloInventario(
                codigo = "MP-002",
                nombre = "Carne Molida Guisada / Sazonada",
                tipoInventario = "MATERIA_PRIMA",
                categoria = "Materia Prima",
                unidadMedida = "Kg",
                stockActual = 12.0,
                stockMinimo = 5.0,
                costoUnitario = 2200.0,
                precioVenta = 0.0,
                notas = "Relleno para empanadas y almuerzos"
            )
            val mpQueso = ArticuloInventario(
                codigo = "MP-003",
                nombre = "Queso Blanco Rallado",
                tipoInventario = "MATERIA_PRIMA",
                categoria = "Materia Prima",
                unidadMedida = "Kg",
                stockActual = 15.0,
                stockMinimo = 6.0,
                costoUnitario = 1800.0,
                precioVenta = 0.0,
                notas = "Relleno para empanadas, arepas y bollos"
            )
            val mpCafeGrano = ArticuloInventario(
                codigo = "MP-004",
                nombre = "Café en Grano Tostado",
                tipoInventario = "MATERIA_PRIMA",
                categoria = "Materia Prima",
                unidadMedida = "Kg",
                stockActual = 14.0,
                stockMinimo = 4.0,
                costoUnitario = 1200.0,
                precioVenta = 0.0,
                notas = "Para moler en tolva de cafetera espresso"
            )
            val mpLeche = ArticuloInventario(
                codigo = "MP-005",
                nombre = "Leche Entera Líquida",
                tipoInventario = "MATERIA_PRIMA",
                categoria = "Materia Prima",
                unidadMedida = "Litro",
                stockActual = 20.0,
                stockMinimo = 8.0,
                costoUnitario = 500.0,
                precioVenta = 0.0,
                notas = "Para capuccinos y café con leche"
            )
            val mpAceite = ArticuloInventario(
                codigo = "MP-006",
                nombre = "Aceite Vegetal para Freír",
                tipoInventario = "MATERIA_PRIMA",
                categoria = "Materia Prima",
                unidadMedida = "Litro",
                stockActual = 18.0,
                stockMinimo = 5.0,
                costoUnitario = 750.0,
                precioVenta = 0.0,
                notas = "Para freidora de empanadas"
            )

            val idHarina = inventarioDao.insertArticulo(mpHarina).toInt()
            val idCarne = inventarioDao.insertArticulo(mpCarne).toInt()
            val idQueso = inventarioDao.insertArticulo(mpQueso).toInt()
            val idCafeGrano = inventarioDao.insertArticulo(mpCafeGrano).toInt()
            val idLeche = inventarioDao.insertArticulo(mpLeche).toInt()
            val idAceite = inventarioDao.insertArticulo(mpAceite).toInt()

            listOf(
                mpHarina.copy(id = idHarina),
                mpCarne.copy(id = idCarne),
                mpQueso.copy(id = idQueso),
                mpCafeGrano.copy(id = idCafeGrano),
                mpLeche.copy(id = idLeche),
                mpAceite.copy(id = idAceite)
            ).forEach { art ->
                movimientoInventarioDao.insertMovimiento(
                    MovimientoInventario(
                        articuloId = art.id,
                        articuloNombre = art.nombre,
                        tipo = "ENTRADA",
                        cantidad = art.stockActual,
                        stockAnterior = 0.0,
                        stockNuevo = art.stockActual,
                        motivo = "Carga inicial de materia prima",
                        fecha = System.currentTimeMillis() - 86400000L * 3
                    )
                )
            }

            // PRODUCTO TERMINADO (Elaborados en cafetería listos en vitrina)
            val ptEmpanadaCarne = ArticuloInventario(
                codigo = "PT-001",
                nombre = "Empanadas de Carne (Vitrina)",
                tipoInventario = "PRODUCTO_TERMINADO",
                categoria = "Empanadas y Frituras",
                unidadMedida = "Unidad",
                stockActual = 25.0,
                stockMinimo = 10.0,
                costoUnitario = 650.0,
                precioVenta = 1800.0,
                notas = "Listas en vitrina caliente para despacho"
            )
            val ptEmpanadaQueso = ArticuloInventario(
                codigo = "PT-002",
                nombre = "Empanadas de Queso (Vitrina)",
                tipoInventario = "PRODUCTO_TERMINADO",
                categoria = "Empanadas y Frituras",
                unidadMedida = "Unidad",
                stockActual = 20.0,
                stockMinimo = 10.0,
                costoUnitario = 580.0,
                precioVenta = 1600.0,
                notas = "En vitrina caliente recién fritas"
            )
            val ptArepas = ArticuloInventario(
                codigo = "PT-003",
                nombre = "Arepas Asadas para Despacho",
                tipoInventario = "PRODUCTO_TERMINADO",
                categoria = "Arepas y Desayunos",
                unidadMedida = "Unidad",
                stockActual = 15.0,
                stockMinimo = 8.0,
                costoUnitario = 400.0,
                precioVenta = 3200.0,
                notas = "Asadas en budare listas para rellenar"
            )
            val ptBollitos = ArticuloInventario(
                codigo = "PT-004",
                nombre = "Bollitos Aliñados",
                tipoInventario = "PRODUCTO_TERMINADO",
                categoria = "Desayunos",
                unidadMedida = "Unidad",
                stockActual = 18.0,
                stockMinimo = 6.0,
                costoUnitario = 300.0,
                precioVenta = 1200.0,
                notas = "Hervidos al vapor calientes"
            )
            val ptCafeTerminado = ArticuloInventario(
                codigo = "PT-005",
                nombre = "Café Expreso / Taza",
                tipoInventario = "PRODUCTO_TERMINADO",
                categoria = "Cafetería",
                unidadMedida = "Taza",
                stockActual = 50.0,
                stockMinimo = 15.0,
                costoUnitario = 200.0,
                precioVenta = 1500.0,
                notas = "Porciones disponibles para preparar al instante"
            )

            val idPtEmpCarne = inventarioDao.insertArticulo(ptEmpanadaCarne).toInt()
            val idPtEmpQueso = inventarioDao.insertArticulo(ptEmpanadaQueso).toInt()
            val idPtArepas = inventarioDao.insertArticulo(ptArepas).toInt()
            val idPtBollitos = inventarioDao.insertArticulo(ptBollitos).toInt()
            val idPtCafe = inventarioDao.insertArticulo(ptCafeTerminado).toInt()

            listOf(
                ptEmpanadaCarne.copy(id = idPtEmpCarne),
                ptEmpanadaQueso.copy(id = idPtEmpQueso),
                ptArepas.copy(id = idPtArepas),
                ptBollitos.copy(id = idPtBollitos),
                ptCafeTerminado.copy(id = idPtCafe)
            ).forEach { art ->
                movimientoInventarioDao.insertMovimiento(
                    MovimientoInventario(
                        articuloId = art.id,
                        articuloNombre = art.nombre,
                        tipo = "PRODUCCION",
                        cantidad = art.stockActual,
                        stockAnterior = 0.0,
                        stockNuevo = art.stockActual,
                        motivo = "Lote inicial pasado a vitrina de ventas",
                        fecha = System.currentTimeMillis() - 86400000L * 2
                    )
                )
            }

            // RECETAS ESTIPULADAS (Materia prima requerida por 1 unidad de producto terminado)
            val recetas = listOf(
                // Receta Empanada de Carne: 0.08 kg de Harina, 0.06 kg de Carne
                RecetaIngrediente(
                    productoTerminadoId = idPtEmpCarne,
                    materiaPrimaId = idHarina,
                    materiaPrimaNombre = "Harina de Maíz Precocida",
                    cantidadPorUnidad = 0.08,
                    unidadMedida = "Kg"
                ),
                RecetaIngrediente(
                    productoTerminadoId = idPtEmpCarne,
                    materiaPrimaId = idCarne,
                    materiaPrimaNombre = "Carne Molida Guisada / Sazonada",
                    cantidadPorUnidad = 0.06,
                    unidadMedida = "Kg"
                ),
                // Receta Empanada de Queso: 0.08 kg de Harina, 0.05 kg de Queso
                RecetaIngrediente(
                    productoTerminadoId = idPtEmpQueso,
                    materiaPrimaId = idHarina,
                    materiaPrimaNombre = "Harina de Maíz Precocida",
                    cantidadPorUnidad = 0.08,
                    unidadMedida = "Kg"
                ),
                RecetaIngrediente(
                    productoTerminadoId = idPtEmpQueso,
                    materiaPrimaId = idQueso,
                    materiaPrimaNombre = "Queso Blanco Rallado",
                    cantidadPorUnidad = 0.05,
                    unidadMedida = "Kg"
                ),
                // Receta Arepas: 0.12 kg de Harina
                RecetaIngrediente(
                    productoTerminadoId = idPtArepas,
                    materiaPrimaId = idHarina,
                    materiaPrimaNombre = "Harina de Maíz Precocida",
                    cantidadPorUnidad = 0.12,
                    unidadMedida = "Kg"
                ),
                // Receta Bollitos: 0.09 kg de Harina
                RecetaIngrediente(
                    productoTerminadoId = idPtBollitos,
                    materiaPrimaId = idHarina,
                    materiaPrimaNombre = "Harina de Maíz Precocida",
                    cantidadPorUnidad = 0.09,
                    unidadMedida = "Kg"
                ),
                // Receta Café Expreso / Taza: 0.015 kg de Café en Grano
                RecetaIngrediente(
                    productoTerminadoId = idPtCafe,
                    materiaPrimaId = idCafeGrano,
                    materiaPrimaNombre = "Café en Grano Tostado",
                    cantidadPorUnidad = 0.015,
                    unidadMedida = "Kg"
                )
            )
            recetaIngredienteDao.insertIngredientes(recetas)
        }
    }
}


