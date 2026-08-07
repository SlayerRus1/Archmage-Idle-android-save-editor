package com.saveeditor.archmageidle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saveeditor.archmageidle.data.SaveViewModel
import com.saveeditor.archmageidle.data.toPrettyJsonString
import com.saveeditor.archmageidle.ui.components.SectionHeader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Composable
fun RawJsonScreen(modifier: Modifier = Modifier, viewModel: SaveViewModel) {
    val root = viewModel.root ?: return

    var text by remember(root) {
        mutableStateOf(root.toPrettyJsonString())
    }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(vertical = 8.dp)) {
        SectionHeader(
            "Raw JSON Editor",
            "Full save contents. Use this for fields with no dedicated screen (quests, wizards, battle loadout, etc.)."
        )

        if (error != null) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = null },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        )

        Button(
            onClick = {
                try {
                    val parsed = Json.parseToJsonElement(text)
                    val obj = parsed as? JsonObject
                    if (obj == null) {
                        error = "The top level of the JSON must be an object ({ ... })."
                    } else {
                        viewModel.updateRoot(obj)
                        error = null
                    }
                } catch (e: Exception) {
                    error = "Invalid JSON: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Validate & Apply")
        }
    }
}
