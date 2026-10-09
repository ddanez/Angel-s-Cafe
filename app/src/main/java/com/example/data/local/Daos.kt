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

    @Query("DELETE FROM clientes")
    suspend fun deleteAll()
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

    @Query("DELETE FROM platos")
    suspend fun deleteAll()
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

    @Query("DELETE FROM transacciones")
    suspend fun deleteAll()
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

    @Query("DELETE FROM proveedores")
    suspend fun deleteAll()
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

    @Query("DELETE FROM compras")
    suspend fun deleteAll()
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

    @Query("DELETE FROM pagos_proveedor")
    suspend fun deleteAll()
}

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuracion WHERE id = 1 LIMIT 1")
    fun getConfiguracion(): Flow<ConfiguracionComercio?>

    @Query("SELECT * FROM configuracion WHERE id = 1 LIMIT 1")
    suspend fun getConfiguracionSync(): ConfiguracionComercio?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: ConfiguracionComercio)

    @Query("DELETE FROM configuracion")
    suspend fun deleteAll()
}

@Dao
interface InventarioDao {
    @Query("SELECT * FROM inventario ORDER BY nombre ASC")
    fun getAllArticulos(): Flow<List<ArticuloInventario>>

    @Query("SELECT * FROM inventario WHERE tipoInventario = :tipo ORDER BY nombre ASC")
    fun getArticulosPorTipo(tipo: String): Flow<List<ArticuloInventario>>

    @Query("SELECT * FROM inventario WHERE id = :id")
    suspend fun getArticuloById(id: Int): ArticuloInventario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticulo(articulo: ArticuloInventario): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticulos(articulos: List<ArticuloInventario>)

    @Update
    suspend fun updateArticulo(articulo: ArticuloInventario)

    @Delete
    suspend fun deleteArticulo(articulo: ArticuloInventario)

    @Query("SELECT COUNT(*) FROM inventario")
    suspend fun getCount(): Int

    @Query("DELETE FROM inventario")
    suspend fun deleteAll()
}

@Dao
interface MovimientoInventarioDao {
    @Query("SELECT * FROM movimientos_inventario ORDER BY fecha DESC")
    fun getAllMovimientos(): Flow<List<MovimientoInventario>>

    @Query("SELECT * FROM movimientos_inventario WHERE articuloId = :articuloId ORDER BY fecha DESC")
    fun getMovimientosByArticulo(articuloId: Int): Flow<List<MovimientoInventario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovimiento(movimiento: MovimientoInventario): Long

    @Delete
    suspend fun deleteMovimiento(movimiento: MovimientoInventario)

    @Query("DELETE FROM movimientos_inventario")
    suspend fun deleteAll()
}

@Dao
interface RecetaIngredienteDao {
    @Query("SELECT * FROM recetas_ingredientes ORDER BY id ASC")
    fun getAllRecetas(): Flow<List<RecetaIngrediente>>

    @Query("SELECT * FROM recetas_ingredientes WHERE productoTerminadoId = :productoId")
    fun getIngredientesByProducto(productoId: Int): Flow<List<RecetaIngrediente>>

    @Query("SELECT * FROM recetas_ingredientes WHERE productoTerminadoId = :productoId")
    suspend fun getIngredientesByProductoList(productoId: Int): List<RecetaIngrediente>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngrediente(ingrediente: RecetaIngrediente): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredientes(ingredientes: List<RecetaIngrediente>)

    @Delete
    suspend fun deleteIngrediente(ingrediente: RecetaIngrediente)

    @Query("DELETE FROM recetas_ingredientes WHERE productoTerminadoId = :productoId")
    suspend fun deleteIngredientesDeProducto(productoId: Int)

    @Query("SELECT COUNT(*) FROM recetas_ingredientes")
    suspend fun getCount(): Int

    @Query("DELETE FROM recetas_ingredientes")
    suspend fun deleteAll()
}


