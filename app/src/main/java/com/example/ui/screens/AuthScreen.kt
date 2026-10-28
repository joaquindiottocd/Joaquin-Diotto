package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CobranzasRepository
import com.example.ui.components.BrandLogo
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.CDSuccess
import com.example.ui.theme.CDSuccessBg
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: CobranzasRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("joaquin.diotto@capitaldiario.com.ar") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo
            BrandLogo(showSubtitle = false)

            Spacer(modifier = Modifier.height(4.dp))

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ingresá a Cobranzas CD",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ingresá con el mail y la contraseña que te dio administración.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Usuario / Email") },
                        placeholder = { Text("nombre@capitaldiario.com.ar") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = CDPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CDPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = CDDanger,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Por favor ingresá tu email y contraseña."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val result = repository.signIn(email, password)
                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        onAuthSuccess()
                                    },
                                    onFailure = { err ->
                                        errorMessage = err.message ?: "No se pudo iniciar sesión. Verificá los datos."
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_submit_login"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CDPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ingresando…")
                        } else {
                            Text("Ingresar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Demo Mode Button
                    OutlinedButton(
                        onClick = {
                            repository.enableDemoMode()
                            onAuthSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_demo_mode"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CDPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explorar en Modo Demostración", fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = "¿Todavía no tenés usuario? Pedile a administración que te agregue desde la sección Usuarios.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Backend info badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CDSuccess)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Servidor: cobranzascd.freebuff.app",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Herramienta de uso interno · Capital Diario\nLos datos quedan guardados y sincronizados online.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
