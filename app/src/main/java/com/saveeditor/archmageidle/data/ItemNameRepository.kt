package com.saveeditor.archmageidle.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * The save file only stores numeric item/resource IDs — the game itself owns the
 * real names and we have no access to that database. This repository lets the
 * user attach their own nicknames to IDs (e.g. after checking in-game), stored
 * locally on-device in SharedPreferences. Nicknames are purely a local UI
 * convenience: they are never written into the save file itself.
 */
class ItemNameRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("item_nicknames", Context.MODE_PRIVATE)

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

    fun displayName(itemId: String): String = nicknames[itemId] ?: "Item #$itemId"
}
