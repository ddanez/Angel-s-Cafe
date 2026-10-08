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
    private val configuracionDao: ConfiguracionDao
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
    val configuracion: Flow<ConfiguracionComercio> = configuracionDao.getConfiguracion().map {
        it ?: ConfiguracionComercio()
    }

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

    suspend fun seedInitialDataIfNeeded() {
        if (platoDao.getCount() == 0) {
            val samplePlatos = listOf(
                Plato(nombre = "Café Expreso ☕", precio = 1500.0, descripcion = "Café negro aromático tostado medio"),
                Plato(nombre = "Capuccino Especial 🥛", precio = 2500.0, descripcion = "Espresso con espuma de leche sedosa"),
                Plato(nombre = "Empanada de Carne 🥩", precio = 1800.0, descripcion = "Empanada crocante bien sazonada"),
                Plato(nombre = "Empanada de Queso 🧀", precio = 1600.0, descripcion = "Empanada artesanal con queso derretido"),
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
                // Seed sample purchases to immediately show realistic data in CXP and compras
                if (p.nombre.contains("Distribuidora")) {
                    compraDao.insertCompra(
                        Compra(
                            proveedorId = provId,
                            proveedorNombre = p.nombre,
                            fecha = System.currentTimeMillis() - 86400000L * 4,
                            detalle = "Sacos de café en grano (5kg) + Azúcar",
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
                    // Abono parcial a la deuda de lácteos
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
    }
}
