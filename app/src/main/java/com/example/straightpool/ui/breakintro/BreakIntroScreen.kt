package com.example.straightpool.ui.breakintro

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.straightpool.scorer.GamePhase
import com.example.straightpool.scorer.ScorerViewModel

@Composable
fun BreakIntroScreen(
    vm: ScorerViewModel,
    onStart: () -> Unit,   // ← this is the only “continue” callback
    onBack: () -> Unit
) {
    val g = vm.game

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Opening Break", style = MaterialTheme.typography.headlineSmall)
        Text("Breaker: ${g.players[g.breakerIndex].name}")

        when (g.phase) {
            GamePhase.Opening -> {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1) Legal break (no ball) → opponent accepts the table
                    Button(
                        onClick = { vm.openingLegalBreak(); onStart() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Legal break") }

                    // 2) Legal break WITH called ball → breaker keeps shooting
                    Button(
                        onClick = { vm.openingLegalBreakWithBall(); onStart() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Legal break (called ball)") }

                    // 3) Breaking foul −2 → opponent chooses accept/rerack
                    OutlinedButton(
                        onClick = { vm.openingBreakFoul() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Breaking foul −2") }
                }
            }

            GamePhase.AwaitChoiceAfterBreakFoul -> {
                Text("Opponent’s choice after the breaking foul:")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { vm.openingOpponentAcceptsTable(); onStart() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Accept table (start)") }


                    OutlinedButton(
                        onClick = { vm.openingForceRerack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Re-rack (same breaker)") }
                }
            }

            GamePhase.Scoring -> {
                Button(onClick = onStart) { Text("Continue to scoring") }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}
