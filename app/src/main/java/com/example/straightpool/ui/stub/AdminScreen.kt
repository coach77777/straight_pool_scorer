package com.example.straightpool.ui.stub

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerStatsScreen(onBack: () -> Unit) {
    Surface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Player Stats", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Welcome to the Player Stats page (coming soon).")
        }
    }
}

@Composable
fun AdminScreen(onBack: () -> Unit) {
    Surface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Admin", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Welcome to the Admin page (coming soon).")
        }
    }
}


