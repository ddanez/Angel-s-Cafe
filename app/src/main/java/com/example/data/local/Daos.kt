package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nombre ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCliente(cliente: Cliente): Long

    @Update
    suspend fun updateCliente(cliente: Cliente)

    @Delete
    suspend fun deleteCliente(cliente: Cliente)

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getClienteById(id: Int): Cliente?
}

@Dao
interface PlatoDao {
    @Query("SELECT * FROM platos ORDER BY nombre ASC")
    fun getAllPlatos(): Flow<List<Plato>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlato(plato: Plato)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatos(platos: List<Plato>)

    @Update
    suspend fun updatePlato(plato: Plato)

    @Delete
    suspend fun deletePlato(plato: Plato)

    @Query("SELECT COUNT(*) FROM platos")
    suspend fun getCount(): Int
}

@Dao
interface TransaccionDao {
    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    fun getAllTransacciones(): Flow<List<Transaccion>>

    @Query("SELECT * FROM transacciones WHERE clienteId = :clienteId ORDER BY fecha DESC")
    fun getTransaccionesByCliente(clienteId: Int): Flow<List<Transaccion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: Transaccion): Long

    @Delete
    suspend fun deleteTransaccion(transaccion: Transaccion)
}

@Dao
interface ProveedorDao {
    @Query("SELECT * FROM proveedores ORDER BY nombre ASC")
    fun getAllProveedores(): Flow<List<Proveedor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProveedor(proveedor: Proveedor): Long

    @Update
    suspend fun updateProveedor(proveedor: Proveedor)

    @Delete
    suspend fun deleteProveedor(proveedor: Proveedor)

    @Query("SELECT COUNT(*) FROM proveedores")
    suspend fun getCount(): Int
}

@Dao
interface CompraDao {
    @Query("SELECT * FROM compras ORDER BY fecha DESC")
    fun getAllCompras(): Flow<List<Compra>>

    @Query("SELECT * FROM compras WHERE proveedorId = :proveedorId ORDER BY fecha DESC")
    fun getComprasByProveedor(proveedorId: Int): Flow<List<Compra>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompra(compra: Compra): Long

    @Delete
    suspend fun deleteCompra(compra: Compra)
}

@Dao
interface PagoProveedorDao {
    @Query("SELECT * FROM pagos_proveedor ORDER BY fecha DESC")
    fun getAllPagosProveedor(): Flow<List<PagoProveedor>>

    @Query("SELECT * FROM pagos_proveedor WHERE proveedorId = :proveedorId ORDER BY fecha DESC")
    fun getPagosByProveedor(proveedorId: Int): Flow<List<PagoProveedor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagoProveedor(pago: PagoProveedor): Long

    @Delete
    suspend fun deletePagoProveedor(pago: PagoProveedor)
}

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuracion WHERE id = 1 LIMIT 1")
    fun getConfiguracion(): Flow<ConfiguracionComercio?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: ConfiguracionComercio)
}
