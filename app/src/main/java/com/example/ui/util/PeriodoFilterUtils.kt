package com.example.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class TipoPeriodo(val titulo: String, val icono: String) {
    DIA("Día", "📅"),
    SEMANA("Semana", "🗓️"),
    MES("Mes", "📆"),
    ANO("Año", "📊"),
    PERSONALIZADO("Específico", "⏳"),
    TODO("Todo", "🌐")
}

data class FiltroPeriodo(
    val tipo: TipoPeriodo = TipoPeriodo.MES,
    val fechaInicio: Long = calcularInicioPeriodo(TipoPeriodo.MES),
    val fechaFin: Long = calcularFinPeriodo(TipoPeriodo.MES),
    val etiquetaPersonalizada: String? = null
) {
    fun coincide(timestamp: Long): Boolean {
        if (tipo == TipoPeriodo.TODO) return true
        return timestamp in fechaInicio..fechaFin
    }

    val textoDescriptivo: String
        get() {
            if (etiquetaPersonalizada != null) return etiquetaPersonalizada
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfMesAno = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val sdfAno = SimpleDateFormat("yyyy", Locale.getDefault())

            return when (tipo) {
                TipoPeriodo.DIA -> "Hoy (${sdfFecha.format(Date(fechaInicio))})"
                TipoPeriodo.SEMANA -> "Esta Semana (${sdfFecha.format(Date(fechaInicio))} al ${sdfFecha.format(Date(fechaFin))})"
                TipoPeriodo.MES -> "Mes de ${sdfMesAno.format(Date(fechaInicio)).replaceFirstChar { it.uppercase() }}"
                TipoPeriodo.ANO -> "Año ${sdfAno.format(Date(fechaInicio))}"
                TipoPeriodo.PERSONALIZADO -> "Del ${sdfFecha.format(Date(fechaInicio))} al ${sdfFecha.format(Date(fechaFin))}"
                TipoPeriodo.TODO -> "Histórico Completo (Sin filtro de fecha)"
            }
        }

    companion object {
        fun porDefecto(tipo: TipoPeriodo = TipoPeriodo.MES): FiltroPeriodo {
            return FiltroPeriodo(
                tipo = tipo,
                fechaInicio = calcularInicioPeriodo(tipo),
                fechaFin = calcularFinPeriodo(tipo)
            )
        }

        fun crearPersonalizado(inicio: Long, fin: Long, etiqueta: String? = null): FiltroPeriodo {
            val inicioNormalizado = Calendar.getInstance().apply {
                timeInMillis = inicio
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val finNormalizado = Calendar.getInstance().apply {
                timeInMillis = fin
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            return FiltroPeriodo(
                tipo = TipoPeriodo.PERSONALIZADO,
                fechaInicio = inicioNormalizado,
                fechaFin = finNormalizado,
                etiquetaPersonalizada = etiqueta
            )
        }
    }
}

fun calcularInicioPeriodo(tipo: TipoPeriodo): Long {
    val cal = Calendar.getInstance()
    when (tipo) {
        TipoPeriodo.DIA -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        TipoPeriodo.SEMANA -> {
            // Primer día de la semana (Lunes o configurado)
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        TipoPeriodo.MES -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        TipoPeriodo.ANO -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        TipoPeriodo.PERSONALIZADO -> {
            // Por defecto últimos 30 días
            cal.add(Calendar.DAY_OF_YEAR, -30)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        TipoPeriodo.TODO -> {
            return 0L
        }
    }
    return cal.timeInMillis
}

fun calcularFinPeriodo(tipo: TipoPeriodo): Long {
    val cal = Calendar.getInstance()
    when (tipo) {
        TipoPeriodo.DIA -> {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        TipoPeriodo.SEMANA -> {
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.add(Calendar.DAY_OF_YEAR, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        TipoPeriodo.MES -> {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        TipoPeriodo.ANO -> {
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        TipoPeriodo.PERSONALIZADO -> {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        TipoPeriodo.TODO -> {
            return Long.MAX_VALUE
        }
    }
    return cal.timeInMillis
}

/**
 * Componente visual reutilizable para filtrar procesos y reportes por período:
 * Día, Semana, Mes, Año, Periodo Específico y Todo.
 */
@Composable
fun SelectorPeriodoBar(
    filtro: FiltroPeriodo,
    onFiltroCambiado: (FiltroPeriodo) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarDialogoRango by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Fila de Chips de Período con Scroll Horizontal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TipoPeriodo.values().forEach { tipo ->
                val seleccionado = filtro.tipo == tipo
                FilterChip(
                    selected = seleccionado,
                    onClick = {
                        if (tipo == TipoPeriodo.PERSONALIZADO) {
                            mostrarDialogoRango = true
                        } else {
                            onFiltroCambiado(FiltroPeriodo.porDefecto(tipo))
                        }
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tipo.icono, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tipo.titulo, fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CafeDarkBrown,
                        selectedLabelColor = SmoothBeige
                    ),
                    modifier = Modifier.testTag("filtro_periodo_${tipo.name.lowercase()}")
                )
            }
        }

        // Barra informativa de período actual activo
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (filtro.tipo == TipoPeriodo.PERSONALIZADO) {
                        mostrarDialogoRango = true
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = CafeBrown,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = filtro.textoDescriptivo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filtro.tipo == TipoPeriodo.PERSONALIZADO) {
                    TextButton(
                        onClick = { mostrarDialogoRango = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Modificar", fontSize = 11.sp, color = CafeDarkBrown, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostrarDialogoRango) {
        DialogoSelectorRangoFechas(
            fechaInicioActual = filtro.fechaInicio,
            fechaFinActual = filtro.fechaFin,
            onDismiss = { mostrarDialogoRango = false },
            onRangoConfirmado = { inicio, fin, label ->
                mostrarDialogoRango = false
                onFiltroCambiado(FiltroPeriodo.crearPersonalizado(inicio, fin, label))
            }
        )
    }
}

/**
 * Diálogo interactivo para seleccionar período específico personalizado o preseteado.
 */
@Composable
fun DialogoSelectorRangoFechas(
    fechaInicioActual: Long,
    fechaFinActual: Long,
    onDismiss: () -> Unit,
    onRangoConfirmado: (inicio: Long, fin: Long, etiqueta: String?) -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var fechaInicioTexto by remember {
        mutableStateOf(sdf.format(Date(if (fechaInicioActual > 0) fechaInicioActual else System.currentTimeMillis() - 7L * 86400000L)))
    }
    var fechaFinTexto by remember {
        mutableStateOf(sdf.format(Date(if (fechaFinActual > 0 && fechaFinActual < Long.MAX_VALUE) fechaFinActual else System.currentTimeMillis())))
    }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏳", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleccionar Período Específico", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Selecciona un rango rápido o escribe las fechas deseadas (formato DD/MM/AAAA):",
                    fontSize = 12.sp,
                    color = SoftGray
                )

                // Botones de presets rápidos
                Text("Accesos rápidos:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CafeDarkBrown)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            val hoy = System.currentTimeMillis()
                            val hace7 = hoy - 7L * 86400000L
                            fechaInicioTexto = sdf.format(Date(hace7))
                            fechaFinTexto = sdf.format(Date(hoy))
                        },
                        label = { Text("Últimos 7 días", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            val hoy = System.currentTimeMillis()
                            val hace15 = hoy - 15L * 86400000L
                            fechaInicioTexto = sdf.format(Date(hace15))
                            fechaFinTexto = sdf.format(Date(hoy))
                        },
                        label = { Text("Últimos 15 días", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            val hoy = System.currentTimeMillis()
                            val hace30 = hoy - 30L * 86400000L
                            fechaInicioTexto = sdf.format(Date(hace30))
                            fechaFinTexto = sdf.format(Date(hoy))
                        },
                        label = { Text("Últimos 30 días", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            // Mes anterior completo
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.MONTH, -1)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            val inicio = cal.timeInMillis
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                            val fin = cal.timeInMillis
                            fechaInicioTexto = sdf.format(Date(inicio))
                            fechaFinTexto = sdf.format(Date(fin))
                        },
                        label = { Text("Mes Anterior", fontSize = 11.sp) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = fechaInicioTexto,
                    onValueChange = {
                        fechaInicioTexto = it
                        errorMensaje = null
                    },
                    label = { Text("Fecha Desde (Inicio)") },
                    placeholder = { Text("DD/MM/AAAA") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = CafeBrown) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("fecha_inicio_input")
                )

                OutlinedTextField(
                    value = fechaFinTexto,
                    onValueChange = {
                        fechaFinTexto = it
                        errorMensaje = null
                    },
                    label = { Text("Fecha Hasta (Fin)") },
                    placeholder = { Text("DD/MM/AAAA") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = CafeBrown) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("fecha_fin_input")
                )

                errorMensaje?.let { msg ->
                    Text(msg, color = SoftRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        sdf.isLenient = false
                        val inicioDate = sdf.parse(fechaInicioTexto.trim())
                        val finDate = sdf.parse(fechaFinTexto.trim())
                        if (inicioDate == null || finDate == null) {
                            errorMensaje = "Formato de fecha inválido. Usa DD/MM/AAAA"
                            return@Button
                        }
                        if (inicioDate.after(finDate)) {
                            errorMensaje = "La fecha de inicio debe ser anterior a la fecha de fin"
                            return@Button
                        }
                        onRangoConfirmado(
                            inicioDate.time,
                            finDate.time,
                            "Del ${sdf.format(inicioDate)} al ${sdf.format(finDate)}"
                        )
                    } catch (e: Exception) {
                        errorMensaje = "Error en formato de fechas: Ej. 01/10/2026"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                modifier = Modifier.testTag("confirmar_rango_fechas_btn")
            ) {
                Text("Aplicar Período")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
