package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.asLongOrDefault
import com.saveeditor.archmageidle.data.getAt
import com.saveeditor.archmageidle.data.path
import com.saveeditor.archmageidle.ui.Screen

private data class HubEntry(val screen: Screen, val description: String)

private val entries = listOf(
    HubEntry(Screen.Player, "Gold, guild marks, play time"),
    HubEntry(Screen.Resources, "Raw resource amounts"),
    HubEntry(Screen.Skills, "Skill levels & skill points"),
    HubEntry(Screen.Inventory, "Items & material quantities"),
    HubEntry(Screen.Shop, "Purchased upgrade levels"),
    HubEntry(Screen.ConceptUpgrades, "Concept upgrade levels"),
    HubEntry(Screen.Elixirs, "Selected elixirs per skill"),
    HubEntry(Screen.Chronomancy, "Era levels & stability"),
    HubEntry(Screen.MapScreenRoute, "Location conquest progress"),
    HubEntry(Screen.RawJson, "Edit the full save as raw JSON (covers quests, wizards, battle loadout, and everything else)"),
)

@Composable
fun EditorHubScreen(
    modifier: Modifier = Modifier,
    viewModel: SaveViewModel,
    onNavigate: (Screen) -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onOpenDifferentFile: () -> Unit,
) {
    val root = viewModel.root
    val gold = root?.getAt(path("player", "gold")).asLongOrDefault(0)
    val guildMarks = root?.getAt(path("player", "guildMarks")).asLongOrDefault(0)
    var showDiscardConfirm by remember { mutableStateOf(false) }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have unsaved edits. Opening a different file will discard them.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onOpenDifferentFile()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Loaded Save", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Gold: $gold", modifier = Modifier.padding(top = 8.dp))
                    Text("Guild Marks: $guildMarks")
                    if (viewModel.dirty) {
                        Text(
                            "Unsaved changes",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                            Text("Save (overwrite original)")
                        }
                        OutlinedButton(onClick = onSaveAs, modifier = Modifier.fillMaxWidth()) {
                            Text("Save As…")
                        }
                        OutlinedButton(
                            onClick = { if (viewModel.dirty) showDiscardConfirm = true else onOpenDifferentFile() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open a Different File")
                        }
                    }
                }
            }
        }

        items(entries) { entry ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(entry.screen) }
                        .padding(16.dp)
                ) {
                    Text(entry.screen.title, fontWeight = FontWeight.Bold)
                    Text(entry.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
