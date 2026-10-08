package com.example.data.local

import androidx.room.*
import com.example.data.model.Cliente
import com.example.data.model.Plato
import com.example.data.model.Transaccion
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

    @Query("SELECT COUNT(*) FROM platos")
    suspend fun getCount(): Int

    @Delete
    suspend fun deletePlato(plato: Plato)
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
