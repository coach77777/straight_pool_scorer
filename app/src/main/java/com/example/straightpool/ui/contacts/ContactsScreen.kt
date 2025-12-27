package com.example.straightpool.ui.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.PlayerRow
import com.example.straightpool.data.PlayersRepoV2

@Composable
fun ContactsScreen() {
    val ctx = LocalContext.current
    val repo = remember { PlayersRepoV2(ctx) }

    var query by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<PlayerRow>()) }

    fun refresh() {
        players = repo.readAll()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val q = query.trim().lowercase()
    val filtered = players.filter { p ->
        if (q.isEmpty()) true
        else p.name.lowercase().contains(q) || p.roster.toString().contains(q)
    }

    fun dial(phone: String) {
        ctx.startActivity(Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        })
    }

    fun sms(phone: String) {
        ctx.startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
        })
    }

    fun email(addr: String) {
        ctx.startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$addr")
        })
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search") }
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filtered.forEach { p ->
                    ContactCard(
                        p = p,
                        onCall = { p.phone?.let { dial(it) } },
                        onText = { p.phone?.let { sms(it) } },
                        onEmail = { p.email?.let { email(it) } }
                    )
                }

                if (players.isEmpty()) {
                    Text("No players yet. Import players.csv in Admin > Players > Import.")
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    p: PlayerRow,
    onCall: () -> Unit,
    onText: () -> Unit,
    onEmail: () -> Unit
) {
    Surface(tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (p.isBye) "${p.name} (BYE)" else p.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCall,
                    enabled = !p.phone.isNullOrBlank() && !p.isBye,
                    modifier = Modifier.weight(1f)
                ) { Text("Call") }

                OutlinedButton(
                    onClick = onText,
                    enabled = !p.phone.isNullOrBlank() && !p.isBye,
                    modifier = Modifier.weight(1f)
                ) { Text("Text") }

                Button(
                    onClick = onEmail,
                    enabled = !p.email.isNullOrBlank() && !p.isBye,
                    modifier = Modifier.weight(1f)
                ) { Text("Email") }
            }
        }
    }
}
