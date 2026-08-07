package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.asBoolOrDefault
import com.saveeditor.archmageidle.data.boolToJsonPrimitive
import com.saveeditor.archmageidle.data.getAt
import com.saveeditor.archmageidle.data.isValidJsonNumber
import com.saveeditor.archmageidle.data.numberToJsonPrimitive
import com.saveeditor.archmageidle.data.path
import com.saveeditor.archmageidle.data.updated
import com.saveeditor.archmageidle.data.withFields
import com.saveeditor.archmageidle.ui.components.SectionHeader
import com.saveeditor.archmageidle.ui.components.SwitchRow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class EraRow(val index: Int, val id: String, var level: String, var stabilityPoints: String)

@Composable
fun ChronomancyScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val erasPath = path("chronomancy", "eras")
    val warpPath = path("chronomancy", "warpControlEnabled")

    val rows = remember(root) {
        val arr = root.getAt(erasPath) as? JsonArray
        val initial = arr?.mapIndexedNotNull { i, entry ->
            val obj = entry as? JsonObject ?: return@mapIndexedNotNull null
            EraRow(
                index = i,
                id = obj["id"]?.jsonPrimitive?.content ?: "?",
                level = obj["level"]?.jsonPrimitive?.content ?: "0",
                stabilityPoints = obj["stabilityPoints"]?.jsonPrimitive?.content ?: "0"
            )
        } ?: emptyList()
        initial.toMutableStateList()
    }

    var warpEnabled by remember(root) { mutableStateOf(root.getAt(warpPath).asBoolOrDefault(false)) }

    val allValid = rows.all { isValidJsonNumber(it.level) && isValidJsonNumber(it.stabilityPoints) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("Chronomancy", "Era levels and stability")

        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Column(Modifier.padding(8.dp)) {
                SwitchRow("Warp Control Enabled", warpEnabled, { warpEnabled = it })
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows.size) { i ->
                val row = rows[i]
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Era ${row.id}", modifier = Modifier.weight(0.7f))
                        OutlinedTextField(
                            value = row.level,
                            onValueChange = { rows[i] = row.copy(level = it) },
                            label = { Text("Level") },
                            singleLine = true,
                            isError = !isValidJsonNumber(row.level),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = row.stabilityPoints,
                            onValueChange = { rows[i] = row.copy(stabilityPoints = it) },
                            label = { Text("Stability") },
                            singleLine = true,
                            isError = !isValidJsonNumber(row.stabilityPoints),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Button(
            enabled = allValid,
            onClick = {
                val arr = root.getAt(erasPath) as JsonArray
                val newList = arr.toMutableList()
                for (row in rows) {
                    val original = newList[row.index] as JsonObject
                    newList[row.index] = original.withFields(
                        "level" to numberToJsonPrimitive(row.level),
                        "stabilityPoints" to numberToJsonPrimitive(row.stabilityPoints)
                    )
                }
                var updated = root.updated(erasPath, JsonArray(newList))
                updated = updated.updated(warpPath, boolToJsonPrimitive(warpEnabled))
                viewModel.updateRoot(updated)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
