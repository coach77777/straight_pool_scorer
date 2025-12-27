package com.example.straightpool.ui.setup

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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.PlayersRepoV2
import com.example.straightpool.data.RosterPlayer
import com.example.straightpool.data.loadLeagueMatchesFromAssets
import com.example.straightpool.standings.calculateStandings

@Composable
fun StandingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    val repo = remember { PlayersRepoV2(ctx) }
    var players by remember { mutableStateOf<List<RosterPlayer>>(emptyList()) }

    val matches = remember { loadLeagueMatchesFromAssets(ctx) }

    LaunchedEffect(Unit) {
        players = repo.readAll()
            .filter { !it.isBye } // optional
            .map { pr -> RosterPlayer(playerId = pr.roster, name = pr.name) }
    }

    val rows = remember(players, matches) {
        calculateStandings(players, matches)
    }

    Surface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Standings", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            Text("Counted matches: ${matches.count { it.isPlayed && it.countsForStandings }}")
            Spacer(Modifier.height(12.dp))

            if (players.isEmpty()) {
                Text("No players yet. Import players.csv in Admin > Players > Import.")
                Spacer(Modifier.height(12.dp))
            }

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                rows.forEach { r ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${r.roster}. ${r.name}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("W: ${r.wins}  L: ${r.losses}  GP: ${r.played}")
                            Text("PF: ${r.pointsFor}  PA: ${r.pointsAgainst}")
                        }
                    }
                }
            }
        }
    }
}
