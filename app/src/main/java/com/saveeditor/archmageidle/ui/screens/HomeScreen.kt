package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.LoadState
import com.saveeditor.archmageidle.data.SaveViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: SaveViewModel,
    onOpenFile: () -> Unit,
    onContinueToEditor: () -> Unit,
) {
    val state = viewModel.loadState

    LaunchedEffect(state) {
        if (state is LoadState.Loaded) {
            onContinueToEditor()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Archmage Save Editor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Decrypt, edit, and re-encrypt your Archmage Idle save files.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        Button(onClick = onOpenFile, modifier = Modifier.fillMaxWidth()) {
            Text("Open Save File (.txt)")
        }

        if (state is LoadState.Loading) {
            Text("Loading…", modifier = Modifier.padding(top = 16.dp))
        }

        if (state is LoadState.Error) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Couldn't load this file",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(state.message, modifier = Modifier.padding(top = 8.dp))
                }
            }
            OutlinedButton(onClick = onOpenFile, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Try Another File")
            }
        }
    }
}
