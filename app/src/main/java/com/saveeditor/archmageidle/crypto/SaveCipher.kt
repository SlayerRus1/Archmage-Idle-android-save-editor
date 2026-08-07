package com.saveeditor.archmageidle.crypto

import android.util.Base64

/**
 * Archmage Idle stores its save files as:
 *   "AT1:" + Base64(payload)
 * where payload = UTF-8 JSON bytes XOR'd against a fixed, repeating
 * 21-byte ASCII key, cycling from the start of the payload.
 *
 * This is a symmetric stream cipher: the same function encrypts and decrypts.
 */
object SaveCipher {

    private const val MAGIC = "AT1:"
    private val KEY = "ArchmageTycoon2026xK9".toByteArray(Charsets.US_ASCII)

    class InvalidSaveException(message: String) : Exception(message)

    private fun xorWithKey(data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        for (i in data.indices) {
            out[i] = (data[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        return out
    }

    /** Decrypts raw save-file text (as read from a .txt file) into a JSON string. */
    fun decrypt(rawFileText: String): String {
        val trimmed = rawFileText.trim()
        if (!trimmed.startsWith(MAGIC)) {
            throw InvalidSaveException("File does not start with the expected \"$MAGIC\" header. This may not be an Archmage Idle save file.")
        }
        val b64 = trimmed.substring(MAGIC.length)
        val payload = try {
            Base64.decode(b64, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            throw InvalidSaveException("Save payload is not valid Base64: ${e.message}")
        }
        val plain = xorWithKey(payload)
        return plain.toString(Charsets.UTF_8)
    }

    /** Encrypts a JSON string back into the "AT1:" save-file text format. */
    fun encrypt(json: String): String {
        val plainBytes = json.toByteArray(Charsets.UTF_8)
        val cipherBytes = xorWithKey(plainBytes)
        val b64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return MAGIC + b64
    }
}
