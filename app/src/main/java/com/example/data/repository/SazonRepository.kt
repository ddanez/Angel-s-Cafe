package com.example.data.repository

import com.example.data.local.ClienteDao
import com.example.data.local.PlatoDao
import com.example.data.local.TransaccionDao
import com.example.data.model.Cliente
import com.example.data.model.Plato
import com.example.data.model.Transaccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ClienteConSaldo(
    val cliente: Cliente,
    val totalCompras: Double,
    val totalAbonos: Double,
    val saldoPendiente: Double
)

class SazonRepository(
    private val clienteDao: ClienteDao,
    private val platoDao: PlatoDao,
    private val transaccionDao: TransaccionDao
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

    val platos: Flow<List<Plato>> = platoDao.getAllPlatos()
    val transacciones: Flow<List<Transaccion>> = transaccionDao.getAllTransacciones()

    fun getTransaccionesDelCliente(clienteId: Int): Flow<List<Transaccion>> =
        transaccionDao.getTransaccionesByCliente(clienteId)

    suspend fun addCliente(cliente: Cliente): Long = clienteDao.insertCliente(cliente)
    suspend fun updateCliente(cliente: Cliente) = clienteDao.updateCliente(cliente)
    suspend fun deleteCliente(cliente: Cliente) = clienteDao.deleteCliente(cliente)

    suspend fun addPlato(plato: Plato) = platoDao.insertPlato(plato)
    suspend fun deletePlato(plato: Plato) = platoDao.deletePlato(plato)

    suspend fun addTransaccion(transaccion: Transaccion): Long =
        transaccionDao.insertTransaccion(transaccion)

    suspend fun deleteTransaccion(transaccion: Transaccion) =
        transaccionDao.deleteTransaccion(transaccion)

    suspend fun seedPlatosIfNeeded() {
        if (platoDao.getCount() == 0) {
            val samplePlatos = listOf(
                Plato(nombre = "Café Expreso ☕", precio = 1500.0, descripcion = "Café negro aromático"),
                Plato(nombre = "Capuccino Especial 🥛", precio = 2500.0, descripcion = "Espresso con espuma de leche sedosa"),
                Plato(nombre = "Empanada de Carne 🥩", precio = 1800.0, descripcion = "Empanada frita bien rellena"),
                Plato(nombre = "Empanada de Queso 🧀", precio = 1600.0, descripcion = "Empanada crocante con queso fundido"),
                Plato(nombre = "Desayuno Completo 🍳", precio = 4500.0, descripcion = "Huevos, tostadas, tocino y café"),
                Plato(nombre = "Almuerzo del Día 🍲", precio = 6500.0, descripcion = "Plato principal con acompañamiento y refresco")
            )
            platoDao.insertPlatos(samplePlatos)
        }
    }
}
