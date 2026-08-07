package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.getAt
import com.saveeditor.archmageidle.data.isValidJsonNumber
import com.saveeditor.archmageidle.data.numberToJsonPrimitive
import com.saveeditor.archmageidle.data.path
import com.saveeditor.archmageidle.data.updated
import com.saveeditor.archmageidle.ui.components.EditorCard
import com.saveeditor.archmageidle.ui.components.LabeledNumberField
import com.saveeditor.archmageidle.ui.components.SectionHeader
import com.saveeditor.archmageidle.ui.components.SwitchRow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean

private fun JsonPrimitive.isBooleanLiteral(): Boolean =
    !isString && (content == "true" || content == "false")

@Composable
fun ShopScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val shopPath = path("shop")
    val shopObj = root.getAt(shopPath) as? JsonObject ?: JsonObject(emptyMap())

    // key -> either a Boolean value or numeric text, tracked in one map for simplicity.
    val boolState = remember(root) {
        mutableStateMapOf<String, Boolean>().apply {
            shopObj.entries.forEach { (k, v) ->
                val p = v as? JsonPrimitive ?: return@forEach
                if (p.isBooleanLiteral()) put(k, p.boolean)
            }
        }
    }
    val numState = remember(root) {
        mutableStateMapOf<String, String>().apply {
            shopObj.entries.forEach { (k, v) ->
                val p = v as? JsonPrimitive ?: return@forEach
                if (!p.isBooleanLiteral()) put(k, p.content)
            }
        }
    }

    val allValid = numState.values.all { isValidJsonNumber(it) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("Shop Upgrades", "Purchased upgrade levels and unlock flags")
        EditorCard {
            numState.keys.sorted().forEach { key ->
                LabeledNumberField(
                    label = key,
                    value = numState[key] ?: "0",
                    onValueChange = { numState[key] = it },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            boolState.keys.sorted().forEach { key ->
                SwitchRow(
                    label = key,
                    checked = boolState[key] ?: false,
                    onCheckedChange = { boolState[key] = it }
                )
            }
        }

        Button(
            enabled = allValid,
            onClick = {
                var updated = shopObj
                val map = LinkedHashMap(updated)
                numState.forEach { (k, v) -> map[k] = numberToJsonPrimitive(v) }
                boolState.forEach { (k, v) -> map[k] = JsonPrimitive(v) }
                updated = JsonObject(map)
                viewModel.updateRoot(root.updated(shopPath, updated))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
