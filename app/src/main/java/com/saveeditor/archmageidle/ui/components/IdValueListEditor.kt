package com.saveeditor.archmageidle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.isValidJsonNumber

data class IdValueRow(var id: String, var value: String, val idEditable: Boolean = true)

/**
 * A scrollable editor for a list of (id, numeric value) rows, with add/remove support.
 * Used for resources, concept upgrades, elixir selections, and similar id->value maps.
 */
@Composable
fun IdValueListEditor(
    rows: SnapshotStateList<IdValueRow>,
    idLabel: String,
    valueLabel: String,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        header?.let { item { it() } }

        items(rows.size) { index ->
            val row = rows[index]
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = row.id,
                        onValueChange = { rows[index] = row.copy(id = it) },
                        label = { Text(idLabel) },
                        singleLine = true,
                        enabled = row.idEditable,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = row.value,
                        onValueChange = { rows[index] = row.copy(value = it) },
                        label = { Text(valueLabel) },
                        singleLine = true,
                        isError = row.value.isNotBlank() && !isValidJsonNumber(row.value),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { rows.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            }
        }

        item {
            Button(
                onClick = { rows.add(IdValueRow("", "0")) },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add Row")
            }
        }
    }
}
