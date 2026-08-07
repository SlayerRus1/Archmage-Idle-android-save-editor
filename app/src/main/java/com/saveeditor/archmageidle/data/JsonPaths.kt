package com.saveeditor.archmageidle.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** A single step into a JSON tree: either an object key or an array index. */
sealed class PathSeg {
    data class Key(val name: String) : PathSeg()
    data class Idx(val index: Int) : PathSeg()
}

fun path(vararg segs: Any): List<PathSeg> = segs.map {
    when (it) {
        is Int -> PathSeg.Idx(it)
        is String -> PathSeg.Key(it)
        else -> error("Unsupported path segment: $it")
    }
}

/** Reads a nested value out of a JsonElement tree, or null if any step is missing. */
fun JsonElement.getAt(p: List<PathSeg>): JsonElement? {
    var cur: JsonElement? = this
    for (seg in p) {
        cur = when (seg) {
            is PathSeg.Key -> (cur as? JsonObject)?.get(seg.name)
            is PathSeg.Idx -> (cur as? JsonArray)?.getOrNull(seg.index)
        } ?: return null
    }
    return cur
}

/**
 * Returns a new tree with the value at [p] replaced by [value].
 * Missing intermediate objects/arrays are created as needed.
 */
fun setAt(node: JsonElement, p: List<PathSeg>, value: JsonElement): JsonElement {
    if (p.isEmpty()) return value
    val seg = p.first()
    val rest = p.drop(1)
    return when (seg) {
        is PathSeg.Key -> {
            val obj = node as? JsonObject ?: JsonObject(emptyMap())
            val child = obj[seg.name] ?: JsonNull
            val updated = LinkedHashMap(obj)
            updated[seg.name] = setAt(child, rest, value)
            JsonObject(updated)
        }
        is PathSeg.Idx -> {
            val arr = node as? JsonArray ?: JsonArray(emptyList())
            val list = arr.toMutableList()
            while (list.size <= seg.index) list.add(JsonNull)
            val child = list[seg.index]
            list[seg.index] = setAt(child, rest, value)
            JsonArray(list)
        }
    }
}

/** Convenience for updating the root object directly. */
fun JsonObject.updated(p: List<PathSeg>, value: JsonElement): JsonObject =
    setAt(this, p, value) as JsonObject

// ---- Numeric / typed accessors -------------------------------------------------

fun JsonElement?.asLongOrDefault(default: Long): Long = (this as? JsonPrimitive)?.longOrNull ?: default
fun JsonElement?.asDoubleOrDefault(default: Double): Double = (this as? JsonPrimitive)?.doubleOrNull ?: default
fun JsonElement?.asBoolOrDefault(default: Boolean): Boolean = (this as? JsonPrimitive)?.booleanOrNull ?: default
fun JsonElement?.asStringOrDefault(default: String): String = (this as? JsonPrimitive)?.contentOrNull ?: default
fun JsonElement?.asNumberText(default: String = "0"): String {
    val p = this as? JsonPrimitive ?: return default
    return p.content
}

/** Regex for a valid bare JSON number (integer or decimal, optional exponent). */
private val NUMBER_REGEX = Regex("""^-?\d+(\.\d+)?([eE][+-]?\d+)?$""")

fun isValidJsonNumber(text: String): Boolean = NUMBER_REGEX.matches(text.trim())

/**
 * Builds a JsonPrimitive number from raw user-entered text, preserving exact
 * formatting where possible (important for very large idle-game currency values).
 */
@OptIn(ExperimentalSerializationApi::class)
fun numberToJsonPrimitive(text: String): JsonPrimitive {
    val t = text.trim()
    t.toLongOrNull()?.let { return JsonPrimitive(it) }
    t.toDoubleOrNull()?.let { return JsonPrimitive(it) }
    // Fallback: preserve arbitrary-precision numeric text verbatim (e.g. huge integers).
    return JsonUnquotedLiteral(t)
}

fun boolToJsonPrimitive(b: Boolean): JsonPrimitive = JsonPrimitive(b)

fun stringToJsonPrimitive(s: String): JsonPrimitive = JsonPrimitive(s)

/** Returns a copy of this object with the given fields overlaid, preserving all other keys. */
fun JsonObject.withFields(vararg fields: Pair<String, JsonElement>): JsonObject {
    val updated = LinkedHashMap(this)
    fields.forEach { (k, v) -> updated[k] = v }
    return JsonObject(updated)
}
