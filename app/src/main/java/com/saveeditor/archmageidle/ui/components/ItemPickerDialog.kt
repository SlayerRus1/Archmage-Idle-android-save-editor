package com.saveeditor.archmageidle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.saveeditor.archmageidle.data.isValidJsonNumber

/**
 * A search-and-pick dialog for choosing an item ID. Since save files only carry
 * numeric IDs (the game owns the real name database, which isn't available to
 * this app), every entry can be labeled with a user-defined nickname right in
 * this list — labeled once, remembered forever, searchable from then on.
 */
@Composable
fun ItemPickerDialog(
    title: String,
    knownIds: List<String>,
    nicknames: SnapshotStateMap<String, String>,
    onSetNickname: (id: String, nickname: String) -> Unit,
    onSelect: (id: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var customId by remember { mutableStateOf("") }

    val filtered = remember(query, knownIds, nicknames.toMap()) {
        val q = query.trim().lowercase()
        val sorted = knownIds.distinct().sortedBy { it.toLongOrNull() ?: Long.MAX_VALUE }
        if (q.isBlank()) sorted
        else sorted.filter { id ->
            id.lowercase().contains(q) || (nicknames[id]?.lowercase()?.contains(q) == true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(16.dp).height(520.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Give an item a nickname so it's easy to find next time. Nicknames are saved on this device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by name or item ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    items(filtered) { id ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nicknames[id] ?: "",
                                onValueChange = { onSetNickname(id, it) },
                                placeholder = { Text("Item #$id") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text("#$id", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { onSelect(id) }) {
                                Icon(Icons.Filled.Check, contentDescription = "Select item #$id")
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No items match your search yet.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Or enter an item ID directly", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customId,
                        onValueChange = { customId = it },
                        label = { Text("Item ID") },
                        singleLine = true,
                        isError = customId.isNotBlank() && !isValidJsonNumber(customId),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = isValidJsonNumber(customId),
                        onClick = { onSelect(customId) }
                    ) {
                        Text("Use")
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
