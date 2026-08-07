package com.saveeditor.archmageidle.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.json.JSONObject

/**
 * Item display names. Real names for ~290 known item IDs are bundled in
 * assets/item_names.json (extracted directly from the game's own client code),
 * covering the large majority of items. For anything not in that list, the
 * user can attach their own local nickname (e.g. after checking in-game),
 * stored in SharedPreferences and taking priority over the bundled name.
 * Nicknames are purely a local UI convenience: they are never written into
 * the save file itself.
 */
class ItemNameRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("item_nicknames", Context.MODE_PRIVATE)

    /** Real names bundled with the app, keyed by item ID string. */
    val knownNames: Map<String, String> = loadBundledNames(appContext)

    /** User-supplied overrides/additions, keyed by item ID string. */
    val nicknames: SnapshotStateMap<String, String> = mutableStateMapOf<String, String>().apply {
        prefs.all.forEach { (k, v) -> if (v is String && v.isNotBlank()) put(k, v) }
    }

    fun setNickname(itemId: String, nickname: String) {
        val trimmed = nickname.trim()
        if (trimmed.isBlank()) {
            nicknames.remove(itemId)
            prefs.edit().remove(itemId).apply()
        } else {
            nicknames[itemId] = trimmed
            prefs.edit().putString(itemId, trimmed).apply()
        }
    }

    fun displayName(itemId: String): String =
        nicknames[itemId] ?: knownNames[itemId] ?: "Item #$itemId"

    /** True if this ID has either a bundled name or a user nickname. */
    fun hasName(itemId: String): Boolean = nicknames.containsKey(itemId) || knownNames.containsKey(itemId)

    private fun loadBundledNames(context: Context): Map<String, String> {
        return try {
            val text = context.assets.open("item_names.json").bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val map = LinkedHashMap<String, String>(json.length())
            json.keys().forEach { key -> map[key] = json.getString(key) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
