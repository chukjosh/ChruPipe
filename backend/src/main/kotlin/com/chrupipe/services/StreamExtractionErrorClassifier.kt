package com.chrupipe.services

object StreamExtractionErrorClassifier {
    fun describe(url: String, throwable: Throwable): String {
        val diagnosticText = buildString {
            append(throwable.message?.trim().orEmpty())
            if (isNotEmpty()) append(' ')
            append(throwable.javaClass.simpleName)
            if (throwable.cause != null) {
                append(' ')
                append(throwable.cause?.message?.trim().orEmpty())
                if (throwable.cause?.message?.trim().isNullOrEmpty() == false) {
                    append(' ')
                    append(throwable.cause?.javaClass?.simpleName.orEmpty())
                }
            }
        }.lowercase()

        val isSoundCloud = url.contains("soundcloud", ignoreCase = true)
        val timedOut = diagnosticText.contains("timed out") ||
            diagnosticText.contains("timeout") ||
            diagnosticText.contains("sockettimeoutexception")
        val blockedOrUnavailable = diagnosticText.contains("client id") ||
            diagnosticText.contains("premium") ||
            diagnosticText.contains("go+") ||
            diagnosticText.contains("protected") ||
            diagnosticText.contains("403") ||
            diagnosticText.contains("429") ||
            diagnosticText.contains("unavailable") ||
            diagnosticText.contains("not available") ||
            diagnosticText.contains("requires premium")

        if (isSoundCloud) {
            if (timedOut) {
                return "SoundCloud is responding slowly or could not be reached. Try again in a moment."
            }
            if (blockedOrUnavailable) {
                return "This SoundCloud track is unavailable, protected, or requires SoundCloud Go+/premium access."
            }
        }

        val message = throwable.message?.trim().orEmpty()
        return if (message.isBlank()) {
            "Failed to extract stream from $url."
        } else {
            "Failed to extract stream from $url: $message"
        }
    }
}
