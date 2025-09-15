package com.example.straightpool.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.RosterPlayer
import com.example.straightpool.data.WeekEntry
import com.example.straightpool.data.loadPlayersFromAssets
import com.example.straightpool.data.loadWeeksFromAssets
import com.example.straightpool.scorer.ScorerViewModel

@Composable
fun SetupScreen(
    vm: ScorerViewModel,
    onStart: (
        target: Int,
        aId: Int?, aName: String,
        bId: Int?, bName: String,
        weekKey: String?, weekLabel: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    // ----- datasets (explicit types!) -----
    var roster by remember { mutableStateOf<List<RosterPlayer>>(emptyList()) }
    var weeks  by remember { mutableStateOf<List<WeekEntry>>(emptyList()) }

    // ----- fields (explicit types!) -----
    var targetStr by remember { mutableStateOf("125") }
    var aSel     by remember { mutableStateOf<Pair<Int?, String>?>(null) }
    var bSel     by remember { mutableStateOf<Pair<Int?, String>?>(null) }
    var wSel     by remember { mutableStateOf<WeekEntry?>(null) }

    // Load CSVs
    LaunchedEffect(Unit) {
        roster = try { loadPlayersFromAssets(ctx) } catch (_: Throwable) { emptyList() }
        weeks  = try { loadWeeksFromAssets(ctx)  } catch (_: Throwable) { emptyList() }
    }

    // Default week once it’s available
    LaunchedEffect(weeks) {
        if (wSel == null && weeks.isNotEmpty()) wSel = weeks.first()
    }

    // Filter lists so A/B can’t pick the same person
    val rosterForA = remember(roster, bSel) { roster.filter { it.playerId != (bSel?.first ?: Int.MIN_VALUE) } }
    val rosterForB = remember(roster, aSel) { roster.filter { it.playerId != (aSel?.first ?: Int.MIN_VALUE) } }

    Surface {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Match Setup", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Back") }
            }

            // Week / Date
            ExposedDropdown(
                label = "Week / Date",
                value = wSel?.let { "${it.key} — ${it.label}" } ?: "",
                items = weeks.map { it.key to "${it.key} — ${it.label}" },
                onPick = { key -> wSel = weeks.firstOrNull { it.key == key } }
            )

            // Player A
            ExposedDropdown(
                label = "Player A",
                value = aSel?.let { "#${it.first ?: "-"}  ${it.second}" } ?: "",
                items = rosterForA.map { it.playerId.toString() to "#${it.playerId}  ${it.name}" },
                onPick = { key ->
                    val p = roster.firstOrNull { it.playerId.toString() == key }
                    aSel = (p?.playerId to (p?.name ?: "Player A"))
                    if (p?.playerId == bSel?.first) bSel = null
                }
            )

            // Player B
            ExposedDropdown(
                label = "Player B",
                value = bSel?.let { "#${it.first ?: "-"}  ${it.second}" } ?: "",
                items = rosterForB.map { it.playerId.toString() to "#${it.playerId}  ${it.name}" },
                onPick = { key ->
                    val p = roster.firstOrNull { it.playerId.toString() == key }
                    bSel = (p?.playerId to (p?.name ?: "Player B"))
                    if (p?.playerId == aSel?.first) aSel = null
                }
            )

            OutlinedTextField(
                value = targetStr,
                onValueChange = { targetStr = it },
                label = { Text("Target score") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        val t = targetStr.toIntOrNull() ?: 125
                        val (aId, aName) = aSel ?: (null to "Player A")
                        val (bId, bName) = bSel ?: (null to "Player B")
                        onStart(t, aId, aName, bId, bName, wSel?.key, wSel?.label)
                    },
                    enabled = aSel != null && bSel != null
                ) { Text("Start Match") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    label: String,
    value: String,
    items: List<Pair<String, String>>,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()  // ok even if deprecated
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (key, labelText) ->
                DropdownMenuItem(
                    text = { Text(labelText) },
                    onClick = { onPick(key); expanded = false }
                )
            }
        }
    }
}
