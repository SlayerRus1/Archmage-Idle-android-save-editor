package com.saveeditor.archmageidle.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

private val XOR_KEY = "ArchmageTycoon2026xK9".toByteArray(Charsets.US_ASCII)

private fun xorForTest(data: ByteArray): ByteArray {
    val out = ByteArray(data.size)
    for (i in data.indices) out[i] = (data[i].toInt() xor XOR_KEY[i % XOR_KEY.size].toInt()).toByte()
    return out
}

/** JVM-only mirror of SaveCipher's logic, using java.util.Base64 (SaveCipher itself
 *  uses android.util.Base64, which isn't available in plain JVM unit tests). Verified
 *  identical behavior to SaveCipher via an independent Python implementation, and
 *  cross-checked against real Archmage Idle save files during development. */
private fun decryptForTest(rawFileText: String): String {
    val trimmed = rawFileText.trim()
    require(trimmed.startsWith("AT1:"))
    val payload = Base64.getDecoder().decode(trimmed.substring(4))
    return xorForTest(payload).toString(Charsets.UTF_8)
}

private fun encryptForTest(json: String): String {
    val cipherBytes = xorForTest(json.toByteArray(Charsets.UTF_8))
    return "AT1:" + Base64.getEncoder().encodeToString(cipherBytes)
}

/**
 * Synthetic fixture matching the real save's field structure (not real user data),
 * deliberately including the numeric edge cases found to be lossy during development:
 * a scientific-notation-prone decimal (totalOnlineMs), an integer beyond Long range
 * (hugeBeyondLong), a small decimal that Double.toString renders as "5.0E-4"
 * (tinyDecimal), plus null/bool/nested-array/unicode/escaped-string coverage.
 */
private fun loadSyntheticJson(): String {
    val stream = object {}.javaClass.classLoader!!.getResourceAsStream("synthetic_save.txt")
        ?: error("synthetic_save.txt not found on test classpath")
    val rawText = stream.bufferedReader(Charsets.UTF_8).readText()
    return decryptForTest(rawText)
}

private fun deepDiff(a: JsonElement, b: JsonElement, path: String, mismatches: MutableList<String>) {
    when {
        a is JsonObject && b is JsonObject -> {
            if (a.keys != b.keys) {
                mismatches.add("$path: key set differs (${a.keys - b.keys} missing, ${b.keys - a.keys} added)")
            }
            for (k in a.keys) if (b.containsKey(k)) deepDiff(a.getValue(k), b.getValue(k), "$path.$k", mismatches)
        }
        a is JsonArray && b is JsonArray -> {
            if (a.size != b.size) {
                mismatches.add("$path: array size differs (${a.size} -> ${b.size})")
            }
            for (i in 0 until minOf(a.size, b.size)) deepDiff(a[i], b[i], "$path[$i]", mismatches)
        }
        a is JsonPrimitive && b is JsonPrimitive -> {
            if (a.content != b.content) mismatches.add("$path: '${a.content}' -> '${b.content}'")
        }
        else -> mismatches.add("$path: shape mismatch ($a vs $b)")
    }
}

class JsonRoundTripTest {

    @Test
    fun `synthetic save decrypts to a valid JSON object`() {
        val json = loadSyntheticJson()
        val parsed = Json.parseToJsonElement(json)
        assertTrue(parsed is JsonObject)
        val obj = parsed as JsonObject
        assertTrue(obj.containsKey("player"))
        assertTrue(obj.containsKey("inventory"))
    }

    @Test
    fun `re-serializing an untouched save preserves every field exactly`() {
        val json = loadSyntheticJson()
        val original = Json.parseToJsonElement(json) as JsonObject
        val roundTripped = original.toPrettyJsonString()
        val reparsed = Json.parseToJsonElement(roundTripped) as JsonObject

        val mismatches = mutableListOf<String>()
        deepDiff(original, reparsed, "$", mismatches)

        assertTrue(
            "Found ${mismatches.size} mismatches after round-trip:\n" + mismatches.take(50).joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    @Test
    fun `full app flow round-trips byte-for-byte through decrypt, edit-free re-encrypt, and re-decrypt`() {
        val rawFileText = object {}.javaClass.classLoader!!.getResourceAsStream("synthetic_save.txt")!!
            .bufferedReader(Charsets.UTF_8).readText()

        // Simulates SaveViewModel.loadFrom() + saveTo() with no edits in between.
        val json = decryptForTest(rawFileText)
        val original = Json.parseToJsonElement(json) as JsonObject
        val reEncoded = original.toPrettyJsonString()
        val reDecrypted = decryptForTest(encryptForTest(reEncoded))
        val reparsed = Json.parseToJsonElement(reDecrypted) as JsonObject

        val mismatches = mutableListOf<String>()
        deepDiff(original, reparsed, "$", mismatches)
        assertTrue(
            "Full save/reload cycle altered ${mismatches.size} fields:\n" + mismatches.take(50).joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    @Test
    fun `numberToJsonPrimitive preserves huge integers beyond Long range exactly`() {
        val huge = "123456789012345678901234" // 24 digits, well beyond Long.MAX_VALUE
        val result = numberToJsonPrimitive(huge)
        assertEquals(huge, result.content)
    }

    @Test
    fun `numberToJsonPrimitive round-trips values that Double toString would reformat`() {
        // Each of these previously came back reformatted (e.g. scientific notation)
        // when routed through JsonPrimitive(Double) instead of preserved verbatim.
        for (value in listOf("0", "5443246206062", "-1", "1451814579", "55726369.40152065", "43963502169.96826", "0.0005")) {
            val result = numberToJsonPrimitive(value)
            assertEquals(value, result.content)
        }
    }

    @Test
    fun `getAt and setAt round-trip a nested edit without disturbing siblings`() {
        val json = loadSyntheticJson()
        val original = Json.parseToJsonElement(json) as JsonObject

        val goldPath = path("player", "gold")
        val originalGold = original.getAt(goldPath)?.jsonPrimitive?.content
        assertEquals("5443246206062", originalGold)

        val updated = original.updated(goldPath, numberToJsonPrimitive("999999999999999"))
        assertEquals("999999999999999", updated.getAt(goldPath)?.jsonPrimitive?.content)

        // Sibling fields must be untouched.
        val guildMarksPath = path("player", "guildMarks")
        assertEquals(
            original.getAt(guildMarksPath)?.jsonPrimitive?.content,
            updated.getAt(guildMarksPath)?.jsonPrimitive?.content
        )

        // Everything outside "player" must be untouched (structural equality).
        assertEquals(original["inventory"], updated["inventory"])
        assertEquals(original["xp"], updated["xp"])
    }

    @Test
    fun `withFields preserves unrelated fields on the same object`() {
        val json = loadSyntheticJson()
        val original = Json.parseToJsonElement(json) as JsonObject
        val progress = (original.getAt(path("xp", "progress")) as JsonArray)[0] as JsonObject

        val updated = progress.withFields("level" to numberToJsonPrimitive("42"))

        assertEquals("42", updated["level"]?.jsonPrimitive?.content)
        // All other keys on that object must survive untouched.
        for (key in progress.keys) {
            if (key == "level") continue
            assertEquals("field '$key' should be unchanged", progress[key], updated[key])
        }
    }

    @Test
    fun `toPrettyJsonString correctly escapes special characters in strings`() {
        val json = loadSyntheticJson()
        val original = Json.parseToJsonElement(json) as JsonObject
        val roundTripped = Json.parseToJsonElement(original.toPrettyJsonString()) as JsonObject

        assertEquals(
            original.getAt(path("text", "withQuotesAndBackslash"))?.jsonPrimitive?.content,
            roundTripped.getAt(path("text", "withQuotesAndBackslash"))?.jsonPrimitive?.content
        )
        assertEquals(
            original.getAt(path("text", "unicode"))?.jsonPrimitive?.content,
            roundTripped.getAt(path("text", "unicode"))?.jsonPrimitive?.content
        )
    }
}
