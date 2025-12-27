package com.example.straightpool.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.straightpool.R
import androidx.compose.ui.text.font.FontWeight

@Composable
fun StartScreen(
    onStart: () -> Unit,
    onContacts: () -> Unit,
    onStats: () -> Unit,
    onAdmin: () -> Unit
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.rs_logo),
                contentDescription = "Red Shoes Billiards",
                modifier = Modifier
                    .width(180.dp)
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                "Straight Pool 14.1",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onContacts, modifier = Modifier.fillMaxWidth()) { Text("Contacts") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth()) { Text("Player Stats") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) { Text("Admin") }
        }
    }
}
