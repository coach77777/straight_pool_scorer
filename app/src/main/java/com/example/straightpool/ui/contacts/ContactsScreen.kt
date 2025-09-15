package com.example.straightpool.ui.contacts

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.straightpool.data.RosterPlayer
import com.example.straightpool.data.loadRosterFromAssets


@Composable
fun ContactsScreen() {
    val context = LocalContext.current

    var roster by remember { mutableStateOf<List<RosterPlayer>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { loadRosterFromAssets(context) }
            .onSuccess { roster = it }
            .onFailure { loadError = it.message ?: "Unknown error" }
    }

    var selectedIndex by remember(roster) {
        mutableStateOf(roster.indexOfFirst { !it.name.equals("BYE", true) }.let { if (it >= 0) it else 0 })
    }
    val selected = roster.getOrNull(selectedIndex)

    var showPicker by remember { mutableStateOf(false) }

    Surface {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Contacts", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    readOnly = true,
                    value = selected?.let { "#${it.playerId}  ${it.name}" } ?: "",
                    onValueChange = {},
                    label = { Text("Select player") },
                    trailingIcon = { Text(if (showPicker) "▲" else "▼") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable { if (roster.isNotEmpty()) showPicker = true }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Loaded: ${roster.size} players")

            loadError?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error loading: $it", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            selected?.let { p ->
                Text("Player: #${p.playerId} ${p.name}", style = MaterialTheme.typography.titleMedium)
                Text("Phone: ${p.phone ?: "—"}")
                Text("Email: ${p.email ?: "—"}")

                Spacer(Modifier.height(16.dp))

                val isBye = p.name.equals("BYE", true)
                val hasPhone = !p.phone.isNullOrBlank()
                val hasEmail = !p.email.isNullOrBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = {
                            val phone = p.phone ?: return@Button
                            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                        },
                        enabled = !isBye && hasPhone
                    ) { Text("Call") }

                    OutlinedButton(
                        onClick = {
                            val phone = p.phone ?: return@OutlinedButton
                            context.startActivity(Intent(Intent.ACTION_VIEW, "sms:$phone".toUri()))
                        },
                        enabled = !isBye && hasPhone
                    ) { Text("Text") }

                    OutlinedButton(
                        onClick = {
                            val email = p.email ?: return@OutlinedButton
                            context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri()))
                        },
                        enabled = !isBye && hasEmail
                    ) { Text("Email") }
                }

                if (isBye) {
                    Spacer(Modifier.height(8.dp))
                    Text("BYE (no contact actions)")
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select a player") },
            text = {
                LazyColumn(Modifier.fillMaxWidth().height(550.dp)) {
                    items(roster) { p ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIndex = roster.indexOf(p)
                                    showPicker = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text("#${p.playerId}  ${p.name}", style = MaterialTheme.typography.titleMedium)
                            Text("Phone: ${p.phone ?: "—"}   •   Email: ${p.email ?: "—"}")
                        }
                        Divider()
                    }
                }
            },
            confirmButton = {}
        )
    }
}
