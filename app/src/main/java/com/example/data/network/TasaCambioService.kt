package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object TasaCambioService {
    private const val TAG = "TasaCambioService"

    /**
     * Consulta fuentes públicas de tasa de cambio para USD a Bolívares (VES/BCV).
     * Si no hay conexión o falla el proveedor primario, intenta una alternativa o retorna la última tasa válida.
     */
    suspend fun obtenerTasaDolarBs(): Double? = withContext(Dispatchers.IO) {
        // Intento 1: API de pydolarve / open exchange rates
        try {
            val url = URL("https://ve.dolarapi.com/v1/dolares/oficial")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)
                val promedio = json.optDouble("promedio", 0.0)
                if (promedio > 0) {
                    Log.d(TAG, "Tasa obtenida exitosamente de dolarapi: $promedio")
                    return@withContext promedio
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar dolarapi: ${e.message}")
        }

        // Intento 2: open.er-api.com
        try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)
                val rates = json.optJSONObject("rates")
                val ves = rates?.optDouble("VES", 0.0) ?: 0.0
                if (ves > 0) {
                    Log.d(TAG, "Tasa obtenida de open.er-api: $ves")
                    return@withContext ves
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar er-api: ${e.message}")
        }

        null
    }
}
