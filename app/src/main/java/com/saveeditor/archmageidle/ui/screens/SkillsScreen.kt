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
import com.saveeditor.archmageidle.data.withFields
import com.saveeditor.archmageidle.ui.components.SectionHeader
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class SkillRow(val index: Int, val skillId: String, var level: String, var skillPoints: String)

@Composable
fun SkillsScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val progressPath = path("xp", "progress")

    val rows = remember(root) {
        val arr = root.getAt(progressPath) as? JsonArray
        val initial = arr?.mapIndexedNotNull { i, entry ->
            val obj = entry as? JsonObject ?: return@mapIndexedNotNull null
            SkillRow(
                index = i,
                skillId = obj["skillId"]?.jsonPrimitive?.content ?: "?",
                level = obj["level"]?.jsonPrimitive?.content ?: "0",
                skillPoints = obj["skillPoints"]?.jsonPrimitive?.content ?: "0"
            )
        } ?: emptyList()
        initial.toMutableStateList()
    }

    var bulkLevel by remember { mutableStateOf("100") }

    val allValid = rows.all { isValidJsonNumber(it.level) && isValidJsonNumber(it.skillPoints) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("Skills", "${rows.size} skills. Level cap is typically 100.")

        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bulkLevel,
                    onValueChange = { bulkLevel = it },
                    label = { Text("Set all levels to") },
                    singleLine = true,
                    isError = !isValidJsonNumber(bulkLevel),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    enabled = isValidJsonNumber(bulkLevel),
                    onClick = {
                        for (i in rows.indices) rows[i] = rows[i].copy(level = bulkLevel)
                    }
                ) {
                    Text("Apply to All")
                }
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
                        Text("Skill ${row.skillId}", modifier = Modifier.weight(0.8f))
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
                            value = row.skillPoints,
                            onValueChange = { rows[i] = row.copy(skillPoints = it) },
                            label = { Text("Skill Pts") },
                            singleLine = true,
                            isError = !isValidJsonNumber(row.skillPoints),
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
                val arr = (root.getAt(progressPath) as JsonArray)
                val newList = arr.toMutableList()
                for (row in rows) {
                    val original = newList[row.index] as JsonObject
                    newList[row.index] = original.withFields(
                        "level" to numberToJsonPrimitive(row.level),
                        "skillPoints" to numberToJsonPrimitive(row.skillPoints)
                    )
                }
                viewModel.updateRoot(root.updated(progressPath, JsonArray(newList)))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
