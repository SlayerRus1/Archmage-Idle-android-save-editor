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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ResourcesScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val resourcesPath = path("resources", "amounts")

    val rows = remember(root) {
        val arr = root.getAt(resourcesPath) as? JsonArray
        val initial = arr?.mapNotNull { entry ->
            val pair = entry as? JsonArray ?: return@mapNotNull null
            if (pair.size < 2) return@mapNotNull null
            IdValueRow(pair[0].jsonPrimitive.content, pair[1].jsonPrimitive.content)
        } ?: emptyList()
        initial.toMutableStateList()
    }

    val allValid = rows.all { isValidJsonNumber(it.id) && isValidJsonNumber(it.value) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        IdValueListEditor(
            rows = rows,
            idLabel = "Resource ID",
            valueLabel = "Amount",
            modifier = Modifier.weight(1f),
            header = { SectionHeader("Resources", "Raw resource pool amounts, indexed by resource ID") }
        )

        Button(
            enabled = allValid,
            onClick = {
                val newArr = JsonArray(rows.map { r ->
                    JsonArray(listOf(numberToJsonPrimitive(r.id), numberToJsonPrimitive(r.value)))
                })
                viewModel.updateRoot(root.updated(resourcesPath, newArr))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
