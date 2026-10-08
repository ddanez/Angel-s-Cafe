package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Cliente
import com.example.data.model.Plato
import com.example.data.model.Transaccion
import com.example.data.repository.ClienteConSaldo
import com.example.data.repository.SazonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SazonViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SazonRepository(
        clienteDao = db.clienteDao(),
        platoDao = db.platoDao(),
        transaccionDao = db.transaccionDao()
    )

    val clientesConSaldo: StateFlow<List<ClienteConSaldo>> = repository.clientesConSaldo
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

    // State for selected client for detailed view/ledger
    private val _selectedClienteId = MutableStateFlow<Int?>(null)
    val selectedClienteId: StateFlow<Int?> = _selectedClienteId.asStateFlow()

    // Flow of transaction filter for selected client
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

    // Selection of food item cart state
    private val _carrito = MutableStateFlow<Map<Plato, Int>>(emptyMap())
    val carrito: StateFlow<Map<Plato, Int>> = _carrito.asStateFlow()

    init {
        // Seed with sample data on startup if database is empty
        viewModelScope.launch {
            repository.seedPlatosIfNeeded()
        }
    }

    fun selectCliente(id: Int?) {
        _selectedClienteId.value = id
    }

    fun agregarCliente(nombre: String, telefono: String, direccion: String) {
        viewModelScope.launch {
            repository.addCliente(Cliente(nombre = nombre, telefono = telefono, direccion = direccion))
        }
    }

    fun borrarCliente(cliente: Cliente) {
        viewModelScope.launch {
            // Unselect if deleting the currently selected client
            if (_selectedClienteId.value == cliente.id) {
                _selectedClienteId.value = null
            }
            repository.deleteCliente(cliente)
        }
    }

    fun agregarPlato(nombre: String, precio: Double, descripcion: String) {
        viewModelScope.launch {
            repository.addPlato(Plato(nombre = nombre, precio = precio, descripcion = descripcion))
        }
    }

    fun borrarPlato(plato: Plato) {
        viewModelScope.launch {
            repository.deletePlato(plato)
        }
    }

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

    fun registrarVentaCarrito(clienteId: Int) {
        val itemsEnCarrito = _carrito.value
        if (itemsEnCarrito.isEmpty()) return

        val total = itemsEnCarrito.entries.sumOf { it.key.precio * it.value }
        val detalle = itemsEnCarrito.entries.joinToString(", ") { "${it.value}x ${it.key.nombre}" }

        viewModelScope.launch {
            repository.addTransaccion(
                Transaccion(
                    clienteId = clienteId,
                    fecha = System.currentTimeMillis(),
                    detalle = detalle,
                    montoTotal = total,
                    tipo = "COMPRA"
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
}
