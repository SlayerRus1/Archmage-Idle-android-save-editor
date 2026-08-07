package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.ItemNameRepository
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.getAt
import com.saveeditor.archmageidle.data.isValidJsonNumber
import com.saveeditor.archmageidle.data.numberToJsonPrimitive
import com.saveeditor.archmageidle.data.path
import com.saveeditor.archmageidle.data.updated
import com.saveeditor.archmageidle.ui.components.ItemPickerDialog
import com.saveeditor.archmageidle.ui.components.SectionHeader
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive

private data class InventoryRow(var itemId: String, var quantity: String, var extra: String)

@Composable
fun InventoryScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel, itemNames: ItemNameRepository) {
    val root = viewModel.root ?: return
    val itemsPath = path("inventory", "items")

    val rows = remember(root) {
        val arr = root.getAt(itemsPath) as? JsonArray
        val initial = arr?.mapNotNull { entry ->
            val tuple = entry as? JsonArray ?: return@mapNotNull null
            if (tuple.size < 2) return@mapNotNull null
            InventoryRow(
                itemId = tuple[0].jsonPrimitive.content,
                quantity = tuple[1].jsonPrimitive.content,
                extra = if (tuple.size >= 3) tuple[2].jsonPrimitive.content else ""
            )
        } ?: emptyList()
        initial.toMutableStateList()
    }

    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) rows else rows.filter { row ->
        row.itemId.contains(query.trim()) ||
            (itemNames.nicknames[row.itemId]?.contains(query.trim(), ignoreCase = true) == true)
    }

    // null = picker closed; -1 = picking an item to add as a new row; >=0 = picking a
    // replacement item ID for the row at that index in `rows`.
    var pickerTargetIndex by remember { mutableStateOf<Int?>(null) }

    val allValid = rows.all {
        isValidJsonNumber(it.quantity) && (it.extra.isBlank() || isValidJsonNumber(it.extra))
    }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("Inventory", "${rows.size} item stacks. Search by name or item ID.")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by name or item ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { pickerTargetIndex = -1 },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add Item")
            }
            Button(
                onClick = {
                    for (i in rows.indices) {
                        val current = rows[i].quantity.toLongOrNull() ?: 0L
                        rows[i] = rows[i].copy(quantity = (current + 10000L).toString())
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("+10000 to All")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered.size) { i ->
                val row = filtered[i]
                val realIndex = rows.indexOf(row)
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { pickerTargetIndex = realIndex },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(itemNames.displayName(row.itemId), fontWeight = FontWeight.Bold)
                                Text(
                                    "ID ${row.itemId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Filled.Edit, contentDescription = "Rename or change item")
                            IconButton(onClick = { rows.removeAt(realIndex) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = row.quantity,
                                onValueChange = { rows[realIndex] = row.copy(quantity = it) },
                                label = { Text("Qty") },
                                singleLine = true,
                                isError = !isValidJsonNumber(row.quantity),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = row.extra,
                                onValueChange = { rows[realIndex] = row.copy(extra = it) },
                                label = { Text("Extra (optional)") },
                                singleLine = true,
                                isError = row.extra.isNotBlank() && !isValidJsonNumber(row.extra),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Button(
            enabled = allValid && rows.all { it.itemId.isNotBlank() && isValidJsonNumber(it.itemId) },
            onClick = {
                val newArr = JsonArray(rows.map { r ->
                    val elems = mutableListOf(numberToJsonPrimitive(r.itemId), numberToJsonPrimitive(r.quantity))
                    if (r.extra.isNotBlank()) elems.add(numberToJsonPrimitive(r.extra))
                    JsonArray(elems)
                })
                viewModel.updateRoot(root.updated(itemsPath, newArr))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }

    val targetIndex = pickerTargetIndex
    if (targetIndex != null) {
        val knownIds = (rows.map { it.itemId } + itemNames.nicknames.keys).distinct()
        ItemPickerDialog(
            title = if (targetIndex == -1) "Add Item" else "Rename or Change Item",
            knownIds = knownIds,
            nicknames = itemNames.nicknames,
            onSetNickname = { id, nickname -> itemNames.setNickname(id, nickname) },
            onSelect = { selectedId ->
                if (targetIndex == -1) {
                    rows.add(InventoryRow(selectedId, "0", ""))
                } else {
                    rows[targetIndex] = rows[targetIndex].copy(itemId = selectedId)
                }
                pickerTargetIndex = null
            },
            onDismiss = { pickerTargetIndex = null }
        )
    }
}
