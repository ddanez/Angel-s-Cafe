package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = ""
)

@Entity(tableName = "platos")
data class Plato(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val precio: Double,
    val descripcion: String = "",
    val categoria: String = "General"
)

@Entity(tableName = "transacciones")
data class Transaccion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clienteId: Int, // 0 si es venta directa/mostrador de contado
    val fecha: Long,
    val detalle: String,
    val montoTotal: Double,
    val tipo: String // "COMPRA" (crédito), "ABONO" (pago cliente), "VENTA_CONTADO"
)

@Entity(tableName = "proveedores")
data class Proveedor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val empresa: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = ""
)

@Entity(tableName = "compras")
data class Compra(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val proveedorId: Int,
    val proveedorNombre: String = "",
    val fecha: Long,
    val detalle: String,
    val montoTotal: Double,
    val condicion: String = "CONTADO", // "CONTADO" o "CREDITO"
    val montoPagado: Double = 0.0
)

@Entity(tableName = "pagos_proveedor")
data class PagoProveedor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val proveedorId: Int,
    val fecha: Long,
    val detalle: String,
    val monto: Double
)

@Entity(tableName = "configuracion")
data class ConfiguracionComercio(
    @PrimaryKey val id: Int = 1,
    val nombreComercio: String = "Angel's Cafe",
    val monedaSimbolo: String = "$",
    val telefono: String = "",
    val direccion: String = "",
    val mensajeCobro: String = "Hola, le saludamos de Angel's Cafe. Le recordamos cordialmente su saldo pendiente de %MONTO%. ¡Muchas gracias por su preferencia!"
)
