package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.getAt
import com.saveeditor.archmageidle.data.isValidJsonNumber
import com.saveeditor.archmageidle.data.numberToJsonPrimitive
import com.saveeditor.archmageidle.data.path
import com.saveeditor.archmageidle.data.updated
import com.saveeditor.archmageidle.ui.components.IdValueListEditor
import com.saveeditor.archmageidle.ui.components.IdValueRow
import com.saveeditor.archmageidle.ui.components.SectionHeader
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ConceptUpgradesScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val upgradesPath = path("conceptUpgrades", "upgrades")

    val rows = remember(root) {
        val obj = root.getAt(upgradesPath) as? JsonObject
        val initial = obj?.entries?.map { (k, v) -> IdValueRow(k, v.jsonPrimitive.content, idEditable = false) } ?: emptyList()
        initial.toMutableStateList()
    }

    var bulkValue by remember { mutableStateOf("8") }
    val allValid = rows.all { isValidJsonNumber(it.id) && isValidJsonNumber(it.value) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bulkValue,
                    onValueChange = { bulkValue = it },
                    label = { Text("Set all levels to") },
                    singleLine = true,
                    isError = !isValidJsonNumber(bulkValue),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    enabled = isValidJsonNumber(bulkValue),
                    onClick = { for (i in rows.indices) rows[i] = rows[i].copy(value = bulkValue) }
                ) {
                    Text("Apply to All")
                }
            }
        }

        IdValueListEditor(
            rows = rows,
            idLabel = "Concept ID",
            valueLabel = "Level",
            modifier = Modifier.weight(1f),
            header = { SectionHeader("Concept Upgrades", "${rows.size} concept upgrade levels") }
        )

        Button(
            enabled = allValid,
            onClick = {
                val map = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>()
                rows.forEach { r -> if (r.id.isNotBlank()) map[r.id] = numberToJsonPrimitive(r.value) }
                viewModel.updateRoot(root.updated(upgradesPath, JsonObject(map)))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
