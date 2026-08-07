package com.saveeditor.archmageidle.data

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.saveeditor.archmageidle.crypto.SaveCipher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

sealed class LoadState {
    object Empty : LoadState()
    object Loading : LoadState()
    data class Loaded(val fileName: String) : LoadState()
    data class Error(val message: String) : LoadState()
}

class SaveViewModel : ViewModel() {

    var loadState by mutableStateOf<LoadState>(LoadState.Empty)
        private set

    var root by mutableStateOf<JsonObject?>(null)
        private set

    var dirty by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)

    /** Returns true if the file loaded successfully. */
    fun loadFrom(resolver: ContentResolver, uri: Uri): Boolean {
        loadState = LoadState.Loading
        try {
            val rawText = resolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: throw IllegalStateException("Could not open the selected file.")

            val json = SaveCipher.decrypt(rawText)
            val parsed = Json.parseToJsonElement(json)
            val obj = parsed as? JsonObject
                ?: throw SaveCipher.InvalidSaveException("Decrypted save is not a JSON object.")

            root = obj
            dirty = false
            val name = queryDisplayName(resolver, uri) ?: "save.txt"
            loadState = LoadState.Loaded(name)
            statusMessage = "Loaded $name"
            return true
        } catch (e: Exception) {
            loadState = LoadState.Error(e.message ?: "Unknown error while loading the save file.")
            return false
        }
    }

    fun updateRoot(newRoot: JsonObject) {
        root = newRoot
        dirty = true
    }

    /** Clears all state so the user can return to Home and open a different file. */
    fun reset() {
        loadState = LoadState.Empty
        root = null
        dirty = false
        statusMessage = null
    }

    fun saveTo(resolver: ContentResolver, uri: Uri) {
        val current = root ?: return
        try {
            val jsonText = current.toPrettyJsonString()
            val encrypted = SaveCipher.encrypt(jsonText)
            resolver.openOutputStream(uri, "wt")?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { it.write(encrypted) }
            } ?: throw IllegalStateException("Could not open destination file for writing.")
            dirty = false
            statusMessage = "Save written successfully."
        } catch (e: Exception) {
            statusMessage = "Failed to save: ${e.message}"
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
