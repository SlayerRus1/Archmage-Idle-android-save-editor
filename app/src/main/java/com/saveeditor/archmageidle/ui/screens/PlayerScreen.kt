package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.asNumberText
import com.saveeditor.archmageidle.data.getAt
import com.saveeditor.archmageidle.data.isValidJsonNumber
import com.saveeditor.archmageidle.data.numberToJsonPrimitive
import com.saveeditor.archmageidle.data.path
import com.saveeditor.archmageidle.data.updated
import com.saveeditor.archmageidle.ui.components.EditorCard
import com.saveeditor.archmageidle.ui.components.LabeledNumberField
import com.saveeditor.archmageidle.ui.components.SectionHeader

@Composable
fun PlayerScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return

    var gold by remember(root) { mutableStateOf(root.getAt(path("player", "gold")).asNumberText("0")) }
    var guildMarks by remember(root) { mutableStateOf(root.getAt(path("player", "guildMarks")).asNumberText("0")) }
    var totalOnlineMs by remember(root) { mutableStateOf(root.getAt(path("player", "totalOnlineMs")).asNumberText("0")) }
    var totalOfflineMs by remember(root) { mutableStateOf(root.getAt(path("player", "totalOfflineMs")).asNumberText("0")) }

    val allValid = isValidJsonNumber(gold) && isValidJsonNumber(guildMarks) &&
        isValidJsonNumber(totalOnlineMs) && isValidJsonNumber(totalOfflineMs)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        SectionHeader("Player", "Core currencies and play-time stats")
        EditorCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledNumberField("Gold", gold, { gold = it })
                LabeledNumberField("Guild Marks", guildMarks, { guildMarks = it })
                LabeledNumberField(
                    "Total Online (ms)", totalOnlineMs, { totalOnlineMs = it },
                    supportingText = "Milliseconds of active play time"
                )
                LabeledNumberField(
                    "Total Offline (ms)", totalOfflineMs, { totalOfflineMs = it },
                    supportingText = "Milliseconds of accrued idle/offline progress"
                )
            }
        }

        Button(
            enabled = allValid,
            onClick = {
                var updated = root
                updated = updated.updated(path("player", "gold"), numberToJsonPrimitive(gold))
                updated = updated.updated(path("player", "guildMarks"), numberToJsonPrimitive(guildMarks))
                updated = updated.updated(path("player", "totalOnlineMs"), numberToJsonPrimitive(totalOnlineMs))
                updated = updated.updated(path("player", "totalOfflineMs"), numberToJsonPrimitive(totalOfflineMs))
                viewModel.updateRoot(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Apply Changes")
        }
    }
}
