package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object TasaCambioService {
    private const val TAG = "TasaCambioService"

    /**
     * Consulta fuentes públicas de tasa de cambio para USD a Bolívares (VES/BCV).
     * Soporta múltiples endpoints de respaldo con User-Agent y timeouts seguros.
     */
    suspend fun obtenerTasaDolarBs(): Double? = withContext(Dispatchers.IO) {
        // 1. Endpoint primario: DolarAPI oficial BCV
        try {
            val url = URL("https://ve.dolarapi.com/v1/dolares/oficial")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)
                val promedio = json.optDouble("promedio", 0.0)
                if (promedio > 0) {
                    Log.i(TAG, "Tasa oficial obtenida de DolarAPI: $promedio")
                    return@withContext promedio
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DolarAPI oficial fallo: ${e.message}")
        }

        // 2. Endpoint secundario: DolarAPI lista general (busca el oficial o promedio)
        try {
            val url = URL("https://ve.dolarapi.com/v1/dolares")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val array = JSONArray(response)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val fuente = item.optString("fuente", "")
                    if (fuente.equals("oficial", ignoreCase = true)) {
                        val prom = item.optDouble("promedio", 0.0)
                        if (prom > 0) {
                            Log.i(TAG, "Tasa oficial encontrada en array de DolarAPI: $prom")
                            return@withContext prom
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DolarAPI lista fallo: ${e.message}")
        }

        // 3. Endpoint alternativo: open.er-api.com
        try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)
                val rates = json.optJSONObject("rates")
                val ves = rates?.optDouble("VES", 0.0) ?: 0.0
                if (ves > 0) {
                    Log.i(TAG, "Tasa obtenida de open.er-api: $ves")
                    return@withContext ves
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "open.er-api fallo: ${e.message}")
        }

        null
    }
}
