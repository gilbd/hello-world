package com.babymonitor.shared.streaming

/**
 * Cross-platform signaling message types for WebRTC.
 */
sealed class SignalingMessage {
    abstract val roomCode: String

    data class Offer(
        override val roomCode: String,
        val sdp: String
    ) : SignalingMessage()

    data class Answer(
        override val roomCode: String,
        val sdp: String
    ) : SignalingMessage()

    data class IceCandidate(
        override val roomCode: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val candidate: String
    ) : SignalingMessage()

    data class Disconnect(
        override val roomCode: String
    ) : SignalingMessage()

    companion object {
        fun parseJson(json: String): SignalingMessage? {
            // Simple JSON parsing without external dependencies
            return try {
                val type = extractJsonString(json, "type")
                val room = extractJsonString(json, "room") ?: return null

                when (type) {
                    "offer" -> Offer(
                        roomCode = room,
                        sdp = extractJsonString(json, "sdp") ?: return null
                    )
                    "answer" -> Answer(
                        roomCode = room,
                        sdp = extractJsonString(json, "sdp") ?: return null
                    )
                    "ice_candidate" -> IceCandidate(
                        roomCode = room,
                        sdpMid = extractJsonString(json, "sdpMid") ?: "",
                        sdpMLineIndex = extractJsonInt(json, "sdpMLineIndex") ?: 0,
                        candidate = extractJsonString(json, "candidate") ?: return null
                    )
                    "disconnect" -> Disconnect(roomCode = room)
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun extractJsonString(json: String, key: String): String? {
            val pattern = """"$key"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""".toRegex()
            return pattern.find(json)?.groupValues?.get(1)
                ?.replace("\\\"", "\"")
                ?.replace("\\n", "\n")
                ?.replace("\\r", "\r")
        }

        private fun extractJsonInt(json: String, key: String): Int? {
            val pattern = """"$key"\s*:\s*(\d+)""".toRegex()
            return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    fun toJson(): String {
        return when (this) {
            is Offer -> """{"type":"offer","room":"$roomCode","sdp":"${sdp.escapeJson()}"}"""
            is Answer -> """{"type":"answer","room":"$roomCode","sdp":"${sdp.escapeJson()}"}"""
            is IceCandidate -> """{"type":"ice_candidate","room":"$roomCode","sdpMid":"$sdpMid","sdpMLineIndex":$sdpMLineIndex,"candidate":"${candidate.escapeJson()}"}"""
            is Disconnect -> """{"type":"disconnect","room":"$roomCode"}"""
        }
    }
}

private fun String.escapeJson(): String {
    return this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
