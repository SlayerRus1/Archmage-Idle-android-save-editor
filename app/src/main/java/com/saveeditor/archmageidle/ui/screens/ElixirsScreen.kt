package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ElixirsScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val elixirsPath = path("elixirs", "selectedElixirs")

    val rows = remember(root) {
        val obj = root.getAt(elixirsPath) as? JsonObject
        val initial = obj?.entries?.map { (k, v) -> IdValueRow(k, v.jsonPrimitive.content) } ?: emptyList()
        initial.toMutableStateList()
    }

    val allValid = rows.all { isValidJsonNumber(it.id) && isValidJsonNumber(it.value) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        IdValueListEditor(
            rows = rows,
            idLabel = "Skill ID",
            valueLabel = "Elixir Item ID",
            modifier = Modifier.weight(1f),
            header = { SectionHeader("Elixirs", "Selected elixir item per skill") }
        )

        Button(
            enabled = allValid,
            onClick = {
                val map = LinkedHashMap<String, JsonElement>()
                rows.forEach { r -> if (r.id.isNotBlank()) map[r.id] = numberToJsonPrimitive(r.value) }
                viewModel.updateRoot(root.updated(elixirsPath, JsonObject(map)))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
