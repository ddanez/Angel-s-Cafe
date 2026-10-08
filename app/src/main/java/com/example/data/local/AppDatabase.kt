package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Cliente::class,
        Plato::class,
        Transaccion::class,
        Proveedor::class,
        Compra::class,
        PagoProveedor::class,
        ConfiguracionComercio::class,
        ArticuloInventario::class,
        MovimientoInventario::class,
        RecetaIngrediente::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun platoDao(): PlatoDao
    abstract fun transaccionDao(): TransaccionDao
    abstract fun proveedorDao(): ProveedorDao
    abstract fun compraDao(): CompraDao
    abstract fun pagoProveedorDao(): PagoProveedorDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun inventarioDao(): InventarioDao
    abstract fun movimientoInventarioDao(): MovimientoInventarioDao
    abstract fun recetaIngredienteDao(): RecetaIngredienteDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sazon_cafe_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
