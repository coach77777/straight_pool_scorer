package com.example.straightpool.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminSettingsScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val pinRepo = remember { AdminPinRepo(ctx) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun digitsOnly(s: String) = s.filter { it.isDigit() }

    val currentLabel = if (pinRepo.hasCustomPin()) "PIN is set (hidden)." else "PIN is default (7777)."

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Admin Settings", style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Text("CHANGE ADMIN PIN", style = MaterialTheme.typography.labelLarge)

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        errorText = null
                        newPin = digitsOnly(it).take(8)
                    },
                    label = { Text("New PIN (4–8 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        errorText = null
                        confirmPin = digitsOnly(it).take(8)
                    },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val a = newPin
                        val b = confirmPin

                        errorText = when {
                            a.length < 4 -> "PIN must be at least 4 digits."
                            a.length > 8 -> "PIN must be 8 digits or less."
                            a != b -> "PINs do not match."
                            else -> null
                        }

                        if (errorText == null) {
                            pinRepo.setPin(a)
                            newPin = ""
                            confirmPin = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved new PIN", duration = SnackbarDuration.Short)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save New PIN") }

                Spacer(Modifier.height(8.dp))

                Text("CURRENT", style = MaterialTheme.typography.labelLarge)
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentLabel,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}


