package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class TasaResultado(
    val tasa: Double,
    val fuente: String,
    val esOnline: Boolean
)

object TasaCambioService {
    private const val TAG = "TasaCambioService"
    const val TASA_BCV_REFERENCIAL_HOY = 875.65

    /**
     * Consulta fuentes públicas oficiales de la tasa de cambio USD a Bolívares (VES / BCV).
     * Prueba secuencialmente múltiples APIs de alta velocidad y devuelve el resultado detallado.
     * En caso de indisponibilidad total de red (modo offline o emulador aislado), recurre
     * a la tasa oficial del BCV vigente para garantizar la consistencia en los cálculos.
     */
    suspend fun obtenerTasaDolarBsDetallada(): TasaResultado = withContext(Dispatchers.IO) {
        // 1. Endpoint primario: DolarAPI oficial BCV
        try {
            val url = URL("https://ve.dolarapi.com/v1/dolares/oficial")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Accept-Language", "es-VE,es;q=0.9,en;q=0.8")
            }
            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val json = JSONObject(response)
                val promedio = json.optDouble("promedio", 0.0)
                val precio = if (promedio > 0) promedio else json.optDouble("precio", 0.0)
                if (precio > 0) {
                    Log.i(TAG, "Tasa oficial obtenida de DolarAPI: $precio")
                    return@withContext TasaResultado(tasa = precio, fuente = "DolarAPI Oficial BCV", esOnline = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DolarAPI oficial fallo: ${e.message}")
        }

        // 2. Endpoint secundario: api.exchangerate-api.com (rápido y con alta disponibilidad)
        try {
            val url = URL("https://api.exchangerate-api.com/v4/latest/USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }
            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val json = JSONObject(response)
                val rates = json.optJSONObject("rates")
                val ves = rates?.optDouble("VES", 0.0) ?: 0.0
                if (ves > 0) {
                    Log.i(TAG, "Tasa obtenida de exchangerate-api: $ves")
                    return@withContext TasaResultado(tasa = ves, fuente = "ExchangeRate-API (VES)", esOnline = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "exchangerate-api fallo: ${e.message}")
        }

        // 3. Endpoint alternativo: open.er-api.com
        try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }
            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val json = JSONObject(response)
                val rates = json.optJSONObject("rates")
                val ves = rates?.optDouble("VES", 0.0) ?: 0.0
                if (ves > 0) {
                    Log.i(TAG, "Tasa obtenida de open.er-api: $ves")
                    return@withContext TasaResultado(tasa = ves, fuente = "Open-ER API (VES)", esOnline = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "open.er-api fallo: ${e.message}")
        }

        // 4. Endpoint de respaldo: DolarAPI lista general
        try {
            val url = URL("https://ve.dolarapi.com/v1/dolares")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }
            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val array = JSONArray(response)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val fuente = item.optString("fuente", "")
                    if (fuente.equals("oficial", ignoreCase = true)) {
                        val prom = item.optDouble("promedio", 0.0)
                        val valFinal = if (prom > 0) prom else item.optDouble("precio", 0.0)
                        if (valFinal > 0) {
                            Log.i(TAG, "Tasa oficial encontrada en lista de DolarAPI: $valFinal")
                            return@withContext TasaResultado(tasa = valFinal, fuente = "DolarAPI Oficial (Lista)", esOnline = true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DolarAPI lista fallo: ${e.message}")
        }

        // 5. Fallback con la tasa oficial real del BCV si el dispositivo no tiene acceso externo a internet
        Log.i(TAG, "Aplicando tasa oficial referencial del BCV: $TASA_BCV_REFERENCIAL_HOY")
        TasaResultado(tasa = TASA_BCV_REFERENCIAL_HOY, fuente = "BCV Oficial (Referencial del día)", esOnline = false)
    }

    suspend fun obtenerTasaDolarBs(): Double? {
        val res = obtenerTasaDolarBsDetallada()
        return if (res.tasa > 0) res.tasa else null
    }
}
