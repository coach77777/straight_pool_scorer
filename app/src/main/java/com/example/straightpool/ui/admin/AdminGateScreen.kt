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

@Composable
fun AdminGateScreen(
    appName: String,
    defaultPasscode: String = "7777",
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val pinRepo = remember { AdminPinRepo(ctx) }
    val expectedPin = remember { pinRepo.getPin(defaultPasscode) }

    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter iPhone Passcode for \"$appName\"")
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = input,
                onValueChange = {
                    error = false
                    if (it.length <= 8) input = it.filter { c -> c.isDigit() }
                },
                label = { Text("Passcode") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                isError = error,
                modifier = Modifier.fillMaxWidth()
            )

            if (error) {
                Spacer(Modifier.height(6.dp))
                Text("Incorrect passcode", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (input == expectedPin) onSuccess() else {
                            error = true
                            input = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Enter") }
            }
        }
    }
}
