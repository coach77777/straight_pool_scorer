package com.example.straightpool.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdminMenuScreen(
    onBack: () -> Unit,
    onAdminSettings: () -> Unit,
    onPlayers: () -> Unit,
    onSchedule: () -> Unit,
    onFixMatchResults: () -> Unit,
    onPlayerStats: () -> Unit,
    onImportMatches: () -> Unit,
) {
    Surface {
        Column(Modifier.padding(16.dp)) {

            Text(
                "Admin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                Text("Back")
            }

            AdminRow("Admin Settings", onAdminSettings)
            AdminRow("Players", onPlayers)
            AdminRow("Schedule", onSchedule)
            AdminRow("Fix Match Results", onFixMatchResults)
            AdminRow("Player Stats", onPlayerStats)
            AdminRow("Import Matches", onImportMatches)
        }
    }
}

@Composable
private fun AdminRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .clickable(onClick = onClick),
        style = MaterialTheme.typography.titleMedium
    )
}


