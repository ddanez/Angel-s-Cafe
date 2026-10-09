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
    val mensajeCobro: String = "Hola, le saludamos de Angel's Cafe. Le recordamos cordialmente su saldo pendiente de %MONTO%. ¡Muchas gracias por su preferencia!",
    val claveLicencia: String = "", // Clave secreta (nunca mostrada al usuario)
    val titularLicencia: String = "Angel's Cafe & Restaurante",
    val tipoLicencia: String = "Licencia de Prueba Demo (15 días)",
    val estadoLicencia: String = "DEMO", // "DEMO", "ACTIVA", "VENCIDA"
    val fechaActivacion: Long = System.currentTimeMillis(),
    val planLicencia: String = "DEMO", // "DEMO", "MENSUAL", "SEMESTRAL", "ANUAL", "VITALICIA"
    val fechaVencimientoLicencia: Long = System.currentTimeMillis() + (15L * 24L * 60L * 60L * 1000L), // 15 días exactos de prueba inicial
    val fueActivadaConClave: Boolean = false,
    val tasaCambioBs: Double = 54.50, // Tasa de cambio oficial USD -> Bs (Bolívares)
    val fechaActualizacionTasa: Long = System.currentTimeMillis(),
    val autoActualizarTasa: Boolean = true
) {
    /**
     * Determina si el sistema se encuentra bloqueado por vencimiento de licencia.
     * La licencia Demo vence a los 15 días.
     * Las licencias mensuales, semestrales y anuales vencen en su fecha respectiva.
     * La licencia Vitalicia solo es permanente si fue activada con clave.
     */
    fun estaBloqueadaPorLicencia(): Boolean {
        if (fueActivadaConClave && planLicencia == "VITALICIA") return false
        if (fueActivadaConClave && fechaVencimientoLicencia <= 0L) return false
        return System.currentTimeMillis() > fechaVencimientoLicencia
    }

    /**
     * Días restantes de vigencia de la licencia actual.
     * Retorna -1 si es Vitalicia permanente, o el número de días restantes hasta vencer.
     */
    fun diasRestantesLicencia(): Long {
        if (fueActivadaConClave && planLicencia == "VITALICIA") return -1L
        val dif = fechaVencimientoLicencia - System.currentTimeMillis()
        if (dif <= 0) return 0L
        return (dif / (24L * 60L * 60L * 1000L)) + 1L
    }
}

@Entity(tableName = "inventario")
data class ArticuloInventario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigo: String = "",
    val nombre: String,
    val tipoInventario: String = "MATERIA_PRIMA", // "MATERIA_PRIMA" (Insumos/ingredientes) o "PRODUCTO_TERMINADO" (Elaborados para vitrina/venta)
    val categoria: String = "Insumos", // "Insumos", "Bebidas", "Alimentos", "Empaques", "Limpieza", "Otros"
    val unidadMedida: String = "Unidad", // "Kg", "Litro", "Unidad", "Caja", "Paquete", "Gramo", "Porción"
    val stockActual: Double = 0.0,
    val stockMinimo: Double = 5.0,
    val costoUnitario: Double = 0.0,
    val precioVenta: Double = 0.0,
    val notas: String = ""
)

@Entity(tableName = "movimientos_inventario")
data class MovimientoInventario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val articuloId: Int,
    val articuloNombre: String,
    val tipo: String, // "ENTRADA", "SALIDA", "AJUSTE", "PRODUCCION"
    val cantidad: Double,
    val stockAnterior: Double,
    val stockNuevo: Double,
    val motivo: String, // "Compra a proveedor", "Consumo/Venta", "Merma/Pérdida", "Ajuste físico de inventario", "Elaboración para vitrina", "Descuento por receta"
    val fecha: Long = System.currentTimeMillis()
)

// Relación de ingredientes de materia prima para producir 1 unidad de producto terminado
@Entity(tableName = "recetas_ingredientes")
data class RecetaIngrediente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productoTerminadoId: Int, // ID del ArticuloInventario de tipo PRODUCTO_TERMINADO
    val materiaPrimaId: Int, // ID del ArticuloInventario de tipo MATERIA_PRIMA
    val materiaPrimaNombre: String,
    val cantidadPorUnidad: Double, // Cantidad de materia prima requerida para preparar 1 unidad de producto terminado
    val unidadMedida: String = "Kg"
)


