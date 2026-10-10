package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfiguracionComercio
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun LicenciaBloqueadaScreen(
    config: ConfiguracionComercio,
    onActivar: (clave: String, plan: String, titular: String, onResultado: (Boolean, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var claveInput by remember { mutableStateOf("") }
    var titularInput by remember { mutableStateOf(config.titularLicencia) }
    var selectedPlan by remember { mutableStateOf("VITALICIA") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }

    val planes = listOf(
        Triple("MENSUAL", "1 Mes", "30 días de vigencia comercial"),
        Triple("SEMESTRAL", "Semestral", "6 meses (180 días)"),
        Triple("ANUAL", "Anual", "1 año completo (365 días)"),
        Triple("VITALICIA", "Vitalicio", "Licencia permanente sin caducidad")
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Icono de bloqueo de seguridad
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SoftRed.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(2.dp, SoftRed),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = SoftRed,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = "Acceso Bloqueado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SoftRed
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SoftRed.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (config.planLicencia == "DEMO") "⚠️ Su período de prueba Demo de 15 días ha finalizado."
                        else "⚠️ Su licencia comercial ha vencido el ${FormatUtils.formatDateOnly(config.fechaVencimientoLicencia)}.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SoftRed
                    )
                    Text(
                        text = "Para continuar registrando ventas, controlando inventarios, materias primas y reportes, active su licencia ingresando la clave de autorización.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 17.sp
                    )
                }
            }

            // Formulario de Activación de Licencia
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Activar Licencia Comercial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "1. Seleccione la modalidad deseada:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGray
                    )

                    // Planes disponibles
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        planes.forEach { (id, label, desc) ->
                            val isSelected = selectedPlan == id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CafeBrown.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CafeDarkBrown) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlan = id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlan = id },
                                        colors = RadioButtonDefaults.colors(selectedColor = CafeDarkBrown)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(desc, fontSize = 11.sp, color = SoftGray)
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "2. Datos de registro:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGray
                    )

                    OutlinedTextField(
                        value = titularInput,
                        onValueChange = { titularInput = it },
                        label = { Text("Titular / Nombre del Negocio *") },
                        placeholder = { Text("Ej: Angel's Cafe C.A.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bloqueo_titular_input")
                    )

                    OutlinedTextField(
                        value = claveInput,
                        onValueChange = {
                            claveInput = it
                            mensajeError = null
                        },
                        label = { Text("Clave de Autorización *") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        supportingText = {
                            if (mensajeError != null) {
                                Text(mensajeError ?: "", color = SoftRed, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Ingrese la clave secreta provista por el desarrollador")
                            }
                        },
                        isError = mensajeError != null,
                        modifier = Modifier.fillMaxWidth().testTag("bloqueo_clave_input")
                    )

                    Button(
                        onClick = {
                            procesando = true
                            mensajeError = null
                            onActivar(claveInput, selectedPlan, titularInput) { exito, msg ->
                                procesando = false
                                if (!exito) {
                                    mensajeError = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !procesando && claveInput.isNotBlank() && titularInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CafeDarkBrown),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("bloqueo_boton_activar")
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SmoothBeige, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Desbloquear y Activar Sistema", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
