package com.example.straightpool.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.straightpool.data.MatchHistoryRepoV2
import kotlinx.coroutines.launch

@Composable
fun MatchHistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { MatchHistoryRepoV2(ctx) }

    var text by remember { mutableStateOf(repo.exportText()) }

    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Match History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            text = repo.exportText()
                            scope.launch { snackbarHostState.showSnackbar("Refreshed") }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Refresh") }

                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(text))
                            scope.launch { snackbarHostState.showSnackbar("Copied") }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy") }

                    Button(
                        onClick = {
                            repo.clearAll()
                            text = repo.exportText()
                            scope.launch { snackbarHostState.showSnackbar("Cleared") }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear") }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(text)
                    }
                }
            }
        }
    }
}


