package com.chrupipe.services

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamExtractionErrorClassifierTest {
    @Test
    fun `soundcloud client id failure becomes a friendly premium or unavailable message`() {
        val message = StreamExtractionErrorClassifier.describe(
            "https://soundcloud.com/free-music-egypt/tayeh-fel-amaken",
            IllegalArgumentException("Couldn't extract client id")
        )

        assertEquals(
            "This SoundCloud track is unavailable, protected, or requires SoundCloud Go+/premium access.",
            message
        )
    }

    @Test
    fun `soundcloud timeout becomes a retry-friendly message`() {
        val message = StreamExtractionErrorClassifier.describe(
            "https://soundcloud.com/hypernoiz/letmedown",
            IllegalArgumentException("Connection timed out: getsockopt")
        )

        assertEquals(
            "SoundCloud is responding slowly or could not be reached. Try again in a moment.",
            message
        )
    }

    @Test
    fun `generic extraction failures stay informative without changing other services`() {
        val message = StreamExtractionErrorClassifier.describe(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            IllegalArgumentException("video unavailable")
        )

        assertEquals(
            "Failed to extract stream from https://www.youtube.com/watch?v=dQw4w9WgXcQ: video unavailable",
            message
        )
    }

    @Test
    fun `soundcloud socket timeout without a message still uses the retry guidance`() {
        val message = StreamExtractionErrorClassifier.describe(
            "https://soundcloud.com/free-music-egypt/tayeh-fel-amaken",
            java.net.SocketTimeoutException()
        )

        assertEquals(
            "SoundCloud is responding slowly or could not be reached. Try again in a moment.",
            message
        )
    }
}
