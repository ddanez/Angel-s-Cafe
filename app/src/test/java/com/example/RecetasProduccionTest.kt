package com.example

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import com.example.data.local.AppDatabase
import com.example.data.model.ArticuloInventario
import com.example.data.model.RecetaIngrediente
import com.example.data.repository.SazonRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecetasProduccionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SazonRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SazonRepository(
            clienteDao = db.clienteDao(),
            platoDao = db.platoDao(),
            transaccionDao = db.transaccionDao(),
            proveedorDao = db.proveedorDao(),
            compraDao = db.compraDao(),
            pagoProveedorDao = db.pagoProveedorDao(),
            configuracionDao = db.configuracionDao(),
            inventarioDao = db.inventarioDao(),
            movimientoInventarioDao = db.movimientoInventarioDao(),
            recetaIngredienteDao = db.recetaIngredienteDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testElaboracionReceta20EmpanadasPolloDescuentaMateriaPrima() = runBlocking {
        // 1. Registrar Materia Prima en Almacén
        val idHarina = db.inventarioDao().insertArticulo(
            ArticuloInventario(
                nombre = "Harina de Maíz Precocida",
                tipoInventario = "MATERIA_PRIMA",
                unidadMedida = "Kg",
                stockActual = 30.0,
                costoUnitario = 0.45
            )
        ).toInt()

        val idPollo = db.inventarioDao().insertArticulo(
            ArticuloInventario(
                nombre = "Pollo Mechado Guisado",
                tipoInventario = "MATERIA_PRIMA",
                unidadMedida = "Kg",
                stockActual = 12.0,
                costoUnitario = 1.90
            )
        ).toInt()

        val idAceite = db.inventarioDao().insertArticulo(
            ArticuloInventario(
                nombre = "Aceite Vegetal",
                tipoInventario = "MATERIA_PRIMA",
                unidadMedida = "Litro",
                stockActual = 10.0,
                costoUnitario = 0.80
            )
        ).toInt()

        // 2. Registrar Producto Terminado en Vitrina (inicia con 0 empanadas)
        val idPtEmpanadasPollo = db.inventarioDao().insertArticulo(
            ArticuloInventario(
                nombre = "Empanadas de Pollo (Vitrina)",
                tipoInventario = "PRODUCTO_TERMINADO",
                unidadMedida = "Unidad",
                stockActual = 0.0,
                precioVenta = 1.80
            )
        ).toInt()

        // 3. Estipular Receta Base para 1 empanada:
        // 0.08 Kg de Harina, 0.06 Kg de Pollo Mechado, 0.01 L de Aceite
        val ingredientesReceta = listOf(
            RecetaIngrediente(
                productoTerminadoId = idPtEmpanadasPollo,
                materiaPrimaId = idHarina,
                materiaPrimaNombre = "Harina de Maíz Precocida",
                cantidadPorUnidad = 0.08,
                unidadMedida = "Kg"
            ),
            RecetaIngrediente(
                productoTerminadoId = idPtEmpanadasPollo,
                materiaPrimaId = idPollo,
                materiaPrimaNombre = "Pollo Mechado Guisado",
                cantidadPorUnidad = 0.06,
                unidadMedida = "Kg"
            ),
            RecetaIngrediente(
                productoTerminadoId = idPtEmpanadasPollo,
                materiaPrimaId = idAceite,
                materiaPrimaNombre = "Aceite Vegetal",
                cantidadPorUnidad = 0.01,
                unidadMedida = "Litro"
            )
        )
        repository.guardarIngredientesReceta(idPtEmpanadasPollo, ingredientesReceta)

        // 4. Elaborar y pasar 20 empanadas para la vitrina
        val error = repository.elaborarProductoTerminado(
            productoTerminadoId = idPtEmpanadasPollo,
            cantidadElaborada = 20.0,
            motivoProduccion = "Elaboración de 20 empanadas de pollo para vitrina"
        )
        assertNull("La elaboración debe completarse sin errores", error)

        // 5. Verificar que el producto terminado en vitrina subió a 20 unidades
        val ptActualizado = db.inventarioDao().getArticuloById(idPtEmpanadasPollo)
        assertNotNull(ptActualizado)
        assertEquals(20.0, ptActualizado!!.stockActual, 0.001)

        // 6. Verificar que la materia prima se descontó exactamente según la receta:
        // Harina: 30.0 - (20 * 0.08 = 1.6) = 28.40 Kg
        val harinaActualizada = db.inventarioDao().getArticuloById(idHarina)
        assertNotNull(harinaActualizada)
        assertEquals(28.40, harinaActualizada!!.stockActual, 0.001)

        // Pollo: 12.0 - (20 * 0.06 = 1.2) = 10.80 Kg
        val polloActualizado = db.inventarioDao().getArticuloById(idPollo)
        assertNotNull(polloActualizado)
        assertEquals(10.80, polloActualizado!!.stockActual, 0.001)

        // Aceite: 10.0 - (20 * 0.01 = 0.2) = 9.80 Litros
        val aceiteActualizado = db.inventarioDao().getArticuloById(idAceite)
        assertNotNull(aceiteActualizado)
        assertEquals(9.80, aceiteActualizado!!.stockActual, 0.001)

        // 7. Verificar movimientos en el Kárdex
        val movimientos = db.movimientoInventarioDao().getAllMovimientos().first()
        assertTrue(movimientos.any { it.tipo == "PRODUCCION" && it.articuloId == idPtEmpanadasPollo && it.cantidad == 20.0 })
        assertTrue(movimientos.any { it.tipo == "SALIDA" && it.articuloId == idPollo && it.cantidad == 1.2 })
        assertTrue(movimientos.any { it.tipo == "SALIDA" && it.articuloId == idHarina && it.cantidad == 1.6 })
    }
}
