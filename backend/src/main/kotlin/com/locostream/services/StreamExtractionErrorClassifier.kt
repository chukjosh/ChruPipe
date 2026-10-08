package com.locostream.services

object StreamExtractionErrorClassifier {
    fun describe(url: String, throwable: Throwable): String {
        val message = throwable.message?.trim().orEmpty()

        if (url.contains("soundcloud", ignoreCase = true) &&
            (message.contains("client id", ignoreCase = true) ||
                message.contains("premium", ignoreCase = true) ||
                message.contains("go+", ignoreCase = true) ||
                message.contains("protected", ignoreCase = true) ||
                message.contains("403", ignoreCase = true) ||
                message.contains("429", ignoreCase = true) ||
                message.contains("unavailable", ignoreCase = true) ||
                message.contains("timed out", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true))) {
            return if (message.contains("timed out", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true)) {
                "SoundCloud is responding slowly or could not be reached. Try again in a moment."
            } else {
                "This SoundCloud track is unavailable, protected, or requires SoundCloud Go+/premium access."
            }
        }

        return if (message.isBlank()) {
            "Failed to extract stream from $url."
        } else {
            "Failed to extract stream from $url: $message"
        }
    }
}
