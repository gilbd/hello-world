package com.babymonitor.shared.streaming

import kotlin.random.Random

/**
 * Cross-platform room code generator for secure peer connections.
 */
object RoomCodeGenerator {

    private const val CODE_LENGTH = 6
    private const val ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Generate a random room code.
     * Uses characters that are easy to read (no O/0, I/1, etc.)
     */
    fun generate(): String {
        return (1..CODE_LENGTH)
            .map { ALLOWED_CHARS[Random.nextInt(ALLOWED_CHARS.length)] }
            .joinToString("")
    }

    /**
     * Validate a room code format.
     */
    fun isValid(code: String): Boolean {
        if (code.length != CODE_LENGTH) return false
        return code.all { it in ALLOWED_CHARS }
    }

    /**
     * Normalize a room code (uppercase, trim).
     */
    fun normalize(code: String): String {
        return code.trim().uppercase()
    }
}
