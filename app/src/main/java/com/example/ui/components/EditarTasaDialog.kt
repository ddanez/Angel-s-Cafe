package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SoftGray

@Composable
fun EditarTasaDialog(
    tasaActual: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var tasaInput by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", tasaActual)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💵", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajustar Tasa de Cambio Manual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ingresa el valor en Bolívares (Bs.) por cada 1 USD. Todos los módulos, ventas, inventarios, compras y deudas recalcularán automáticamente sus equivalencias en dual.",
                    fontSize = 12.sp,
                    color = SoftGray
                )

                OutlinedTextField(
                    value = tasaInput,
                    onValueChange = { tasaInput = it },
                    label = { Text("Tasa de Cambio (Bs. por 1 USD) *") },
                    placeholder = { Text("Ej: 875.65") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasa_cambio_manual_input")
                )

                // Botón directo para fijar tasa oficial del BCV vigente
                OutlinedButton(
                    onClick = { tasaInput = "875.65" },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏛️ Fijar Oficial BCV Vigente (Bs. 875.65)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Referencias rápidas:",
                    fontSize = 11.sp,
                    color = SoftGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(800.0, 850.0, 875.65, 900.0, 950.0).forEach { r ->
                        SuggestionChip(
                            onClick = { tasaInput = String.format(java.util.Locale.US, "%.2f", r) },
                            label = { Text("Bs. $r", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = tasaInput.replace(",", ".").toDoubleOrNull()
                    if (rate != null && rate > 0) {
                        onConfirm(rate)
                    }
                },
                enabled = (tasaInput.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Guardar Tasa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
