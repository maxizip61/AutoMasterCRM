package ru.automaster.app.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.automaster.app.ui.theme.GraphiteDark
import ru.automaster.app.ui.theme.IndigoPrimary

@Composable
fun AppLockGate(
    isLocked: Boolean,
    useBiometric: Boolean,
    onUnlock: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    content: @Composable () -> Unit
) {
    if (!isLocked) {
        content()
        return
    }

    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showBiometricPrompt by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity

    // Настройка биометрии
    if (useBiometric && activity != null) {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS && !showBiometricPrompt) {
            LaunchedEffect(Unit) {
                showBiometricPrompt = true
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlock()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        showBiometricPrompt = false
                    }
                })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Вход в Автомастер")
                    .setSubtitle("Приложите палец для разблокировки")
                    .setNegativeButtonText("Использовать PIN")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()

                prompt.authenticate(promptInfo)
            }
        }
    }

    // Экран ввода PIN
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphiteDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Введите PIN-код",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 4) {
                        pin = it
                        error = false
                        if (it.length == 4) {
                            if (onVerifyPin(it)) {
                                onUnlock()
                            } else {
                                error = true
                                pin = ""
                            }
                        }
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                ),
                isError = error,
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x1AFFFFFF),
                    unfocusedContainerColor = Color(0x1AFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = IndigoPrimary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Неверный PIN-код", color = MaterialTheme.colorScheme.error)
            }

            if (useBiometric) {
                Spacer(modifier = Modifier.height(32.dp))
                IconButton(onClick = { showBiometricPrompt = false }) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Биометрия",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}