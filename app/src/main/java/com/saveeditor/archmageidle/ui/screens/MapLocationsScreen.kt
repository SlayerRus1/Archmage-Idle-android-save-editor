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
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

private data class LocationRow(
    val index: Int,
    val locationId: String,
    var victories: String,
    var conquered: Boolean,
    var firstTimeRewardClaimed: Boolean,
    var conqueredRewardClaimed: Boolean,
)

@Composable
fun MapLocationsScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return
    val locPath = path("map", "locationProgress")

    val rows = remember(root) {
        val arr = root.getAt(locPath) as? JsonArray
        val initial = arr?.mapIndexedNotNull { i, entry ->
            val obj = entry as? JsonObject ?: return@mapIndexedNotNull null
            LocationRow(
                index = i,
                locationId = obj["locationId"]?.jsonPrimitive?.content ?: "?",
                victories = obj["victories"]?.jsonPrimitive?.content ?: "0",
                conquered = obj["conquered"].asBoolOrDefault(false),
                firstTimeRewardClaimed = obj["firstTimeRewardClaimed"].asBoolOrDefault(false),
                conqueredRewardClaimed = obj["conqueredRewardClaimed"].asBoolOrDefault(false),
            )
        } ?: emptyList()
        initial.toMutableStateList()
    }

    val allValid = rows.all { isValidJsonNumber(it.victories) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader("Map / Locations", "${rows.size} locations")

        Button(
            onClick = {
                for (i in rows.indices) {
                    rows[i] = rows[i].copy(
                        conquered = true,
                        firstTimeRewardClaimed = true,
                        conqueredRewardClaimed = true
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Conquer All Locations")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows.size) { i ->
                val row = rows[i]
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Location ${row.locationId}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = row.victories,
                                onValueChange = { rows[i] = row.copy(victories = it) },
                                label = { Text("Victories") },
                                singleLine = true,
                                isError = !isValidJsonNumber(row.victories),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        SwitchRow("Conquered", row.conquered, { rows[i] = row.copy(conquered = it) })
                        SwitchRow("First-Time Reward Claimed", row.firstTimeRewardClaimed, { rows[i] = row.copy(firstTimeRewardClaimed = it) })
                        SwitchRow("Conquered Reward Claimed", row.conqueredRewardClaimed, { rows[i] = row.copy(conqueredRewardClaimed = it) })
                    }
                }
            }
        }

        Button(
            enabled = allValid,
            onClick = {
                val arr = root.getAt(locPath) as JsonArray
                val newList = arr.toMutableList()
                for (row in rows) {
                    val original = newList[row.index] as JsonObject
                    newList[row.index] = original.withFields(
                        "victories" to numberToJsonPrimitive(row.victories),
                        "conquered" to boolToJsonPrimitive(row.conquered),
                        "firstTimeRewardClaimed" to boolToJsonPrimitive(row.firstTimeRewardClaimed),
                        "conqueredRewardClaimed" to boolToJsonPrimitive(row.conqueredRewardClaimed),
                    )
                }
                viewModel.updateRoot(root.updated(locPath, JsonArray(newList)))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
