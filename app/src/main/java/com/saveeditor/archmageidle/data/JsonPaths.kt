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
 * Builds a JsonPrimitive number from raw user-entered text, preserving the
 * exact text the user typed.
 *
 * This deliberately does NOT go through `JsonPrimitive(Long)`/`JsonPrimitive(Double)`
 * for values that fit: Kotlin's `Double.toString()` switches to scientific
 * notation outside a narrow magnitude range (e.g. "55726369.40152065" becomes
 * "5.572636940152065E7") and loses precision beyond ~15-17 significant digits
 * for large integers — both confirmed against real save data. Since callers
 * only invoke this after `isValidJsonNumber` has already confirmed [text] is
 * syntactically a valid JSON number, we can always emit it as a literal
 * unquoted number and get byte-for-byte fidelity with zero reformatting risk.
 * `toDoubleOrNull()` is used only to reject non-finite results (there are none
 * reachable through the regex, but this keeps the function safe standalone).
 */
@OptIn(ExperimentalSerializationApi::class)
fun numberToJsonPrimitive(text: String): JsonPrimitive {
    val t = text.trim()
    val asDouble = t.toDoubleOrNull()
    return if (asDouble != null && asDouble.isFinite()) {
        JsonUnquotedLiteral(t)
    } else {
        // Not a recognizable number at all (shouldn't happen given callers
        // validate first) — fall back to a safe default rather than emitting
        // invalid JSON.
        JsonPrimitive(0L)
    }
}

fun boolToJsonPrimitive(b: Boolean): JsonPrimitive = JsonPrimitive(b)

fun stringToJsonPrimitive(s: String): JsonPrimitive = JsonPrimitive(s)

/** Returns a copy of this object with the given fields overlaid, preserving all other keys. */
fun JsonObject.withFields(vararg fields: Pair<String, JsonElement>): JsonObject {
    val updated = LinkedHashMap(this)
    fields.forEach { (k, v) -> updated[k] = v }
    return JsonObject(updated)
}

// ---- Verbatim pretty-printing ---------------------------------------------------

/**
 * Serializes this element to indented JSON text, writing every [JsonPrimitive]'s
 * `content` byte-for-byte instead of round-tripping it through a parsed Double/Long.
 *
 * This matters because kotlinx.serialization's own `Json.encodeToString` does NOT
 * preserve original number formatting: it re-renders every numeric primitive via
 * Kotlin's `Double.toString()`, which switches to scientific notation outside a
 * narrow magnitude range (e.g. "55726369.40152065" becomes "5.572636940152065E7",
 * and "0.0005" becomes "5.0E-4"). For a save-file editor that must round-trip
 * fields the user never touched, that's silent data corruption — confirmed by
 * exact-content diffing against a real save file. Hence this hand-written writer.
 */
fun JsonElement.toPrettyJsonString(indent: String = "  "): String {
    val sb = StringBuilder()
    writeJsonElement(this, sb, indent, 0)
    return sb.toString()
}

private fun writeJsonElement(element: JsonElement, sb: StringBuilder, indent: String, depth: Int) {
    when (element) {
        is JsonNull -> sb.append("null")
        is JsonObject -> writeJsonObject(element, sb, indent, depth)
        is JsonArray -> writeJsonArray(element, sb, indent, depth)
        is JsonPrimitive -> {
            if (element.isString) writeJsonString(element.content, sb) else sb.append(element.content)
        }
    }
}

private fun writeJsonObject(obj: JsonObject, sb: StringBuilder, indent: String, depth: Int) {
    if (obj.isEmpty()) {
        sb.append("{}")
        return
    }
    sb.append("{\n")
    val childIndent = indent.repeat(depth + 1)
    val entries = obj.entries.toList()
    for ((i, entry) in entries.withIndex()) {
        sb.append(childIndent)
        writeJsonString(entry.key, sb)
        sb.append(": ")
        writeJsonElement(entry.value, sb, indent, depth + 1)
        if (i != entries.lastIndex) sb.append(',')
        sb.append('\n')
    }
    sb.append(indent.repeat(depth)).append('}')
}

private fun writeJsonArray(arr: JsonArray, sb: StringBuilder, indent: String, depth: Int) {
    if (arr.isEmpty()) {
        sb.append("[]")
        return
    }
    sb.append("[\n")
    val childIndent = indent.repeat(depth + 1)
    for ((i, item) in arr.withIndex()) {
        sb.append(childIndent)
        writeJsonElement(item, sb, indent, depth + 1)
        if (i != arr.lastIndex) sb.append(',')
        sb.append('\n')
    }
    sb.append(indent.repeat(depth)).append(']')
}

private fun writeJsonString(s: String, sb: StringBuilder) {
    sb.append('"')
    for (ch in s) {
        when (ch) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
        }
    }
    sb.append('"')
}
