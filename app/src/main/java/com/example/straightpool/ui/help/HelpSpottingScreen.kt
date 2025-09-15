package com.example.straightpool.ui.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpSpottingScreen(onBack: () -> Unit) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spotting guide (14.1 Straight Pool)",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Text("• Object ball ends in/among the rack → spot on HEAD SPOT.")
                Text("• Cue ball in/among the rack → cue ball in hand BEHIND HEADSTRING (kitchen).")
                Text("• Both in the rack → cue ball on HEAD SPOT, object/break ball on CENTER SPOT.")
                Text("• If a spot is occupied → place on that spot’s line toward the nearest end rail.")
            }
        }
    }
}

