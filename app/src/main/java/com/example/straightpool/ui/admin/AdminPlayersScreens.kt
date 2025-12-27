package com.example.straightpool.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.PlayersRepoV2
import com.example.straightpool.data.PlayerRow
import kotlinx.coroutines.launch

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun AdminPlayersScreen(
    onBack: () -> Unit,
    onEditPlayer: (roster: Int) -> Unit,
    onAddPlayer: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember { PlayersRepoV2(ctx) }

    var query by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<PlayerRow>()) }

    fun refresh() {
        players = repo.readAll()
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filtered = players.filter {
        val q = query.trim().lowercase()
        if (q.isEmpty()) true
        else it.name.lowercase().contains(q) || it.roster.toString().contains(q)
    }

    Scaffold { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Players  ${filtered.size}/${players.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") }
                )

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import") }
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export") }
                    Button(onClick = onAddPlayer, modifier = Modifier.weight(1f)) { Text("Add") }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filtered.forEach { p ->
                        PlayerRowCard(
                            p = p,
                            onEdit = { onEditPlayer(p.roster) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRowCard(
    p: PlayerRow,
    onEdit: () -> Unit
) {
    val ctx = LocalContext.current

    fun openDial(phone: String) {
        val i = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") }
        ctx.startActivity(i)
    }

    fun openSms(phone: String) {
        val i = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:$phone") }
        ctx.startActivity(i)
    }

    fun openEmail(email: String) {
        val i = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$email") }
        ctx.startActivity(i)
    }

    Surface(tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (p.isBye) "${p.name} (BYE)" else p.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { p.phone?.let { openDial(it) } },
                    enabled = !p.phone.isNullOrBlank() && !p.isBye,
                    modifier = Modifier.weight(1f)
                ) { Text("Call") }

                OutlinedButton(
                    onClick = { p.phone?.let { openSms(it) } },
                    enabled = !p.phone.isNullOrBlank() && !p.isBye,
                    modifier = Modifier.weight(1f)
                ) { Text("Text") }

                OutlinedButton(
                    onClick = { p.email?.let { openEmail(it) } },
                    enabled = !p.email.isNullOrBlank() && !p.isBye,
                    modifier = Modifier.weight(1f)
                ) { Text("Email") }

                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
            }
        }
    }
}

@Composable
fun AdminEditPlayerScreen(
    roster: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember { PlayersRepoV2(ctx) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val original = repo.readAll().firstOrNull { it.roster == roster }
        ?: PlayerRow(roster = roster, name = "Player $roster")

    var name by remember { mutableStateOf(original.name) }
    var phone by remember { mutableStateOf(original.phone ?: "") }
    var email by remember { mutableStateOf(original.email ?: "") }
    var isBye by remember { mutableStateOf(original.isBye) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onBack) { Text("Cancel") }
                    Text("Edit Player", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            val final = PlayerRow(
                                roster = roster,
                                name = name.trim().ifEmpty { "Player $roster" },
                                phone = phone.trim().ifEmpty { null },
                                email = email.trim().ifEmpty { null },
                                isBye = isBye
                            )
                            repo.upsert(final)
                            scope.launch { snackbarHostState.showSnackbar("Saved", duration = SnackbarDuration.Short) }
                            onBack()
                        }
                    ) { Text("Save") }
                }

                Text("Roster #: $roster", fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    enabled = !isBye
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isBye
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isBye
                )

                Spacer(Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        isBye = true
                        name = "BYE $roster"
                        phone = ""
                        email = ""
                        scope.launch { snackbarHostState.showSnackbar("Converted to BYE", duration = SnackbarDuration.Short) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Convert to BYE") }

                OutlinedButton(
                    onClick = {
                        repo.delete(roster)
                        scope.launch { snackbarHostState.showSnackbar("Deleted", duration = SnackbarDuration.Short) }
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete Player") }
            }
        }
    }
}

@Composable
fun AdminImportPlayersScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember { PlayersRepoV2(ctx) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val n = repo.replaceFromCsvText(text)
            scope.launch { snackbarHostState.showSnackbar("Imported $n players", duration = SnackbarDuration.Short) }

            onBack()
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("Import failed: ${e.message}", duration = SnackbarDuration.Long) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Import Players", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Text("Choose a CSV file with header: roster,name,phone,email,isBye")

                Button(
                    onClick = { picker.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pick CSV") }
            }
        }
    }
}

@Composable
fun AdminExportPlayersScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember { PlayersRepoV2(ctx) }

    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(repo.exportText()) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Export Players", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { text = repo.exportText() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Refresh") }

                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(text))
                            scope.launch { snackbarHostState.showSnackbar("Copied", duration = SnackbarDuration.Short) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy") }
                }

                Surface(tonalElevation = 1.dp) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text)
                    }
                }
            }
        }
    }
}


