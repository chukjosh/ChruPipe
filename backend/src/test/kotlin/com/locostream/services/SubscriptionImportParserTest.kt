package com.locostream.services

import com.locostream.models.SubscribeRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionImportParserTest {
    @Test
    fun `plain text import trims blank lines and deduplicates urls`() {
        val subscriptions = SubscriptionImportParser.parse(
            "https://www.youtube.com/@alpha\n\nhttps://www.youtube.com/@alpha\nhttps://www.youtube.com/@beta\n",
            "txt"
        )

        assertEquals(
            listOf(
                "https://www.youtube.com/@alpha",
                "https://www.youtube.com/@beta"
            ),
            subscriptions.map { it.channelUrl }
        )
    }

    @Test
    fun `json import parses subscription objects into subscribe requests`() {
        val subscriptions = SubscriptionImportParser.parse(
            """
            [
              {
                "channelId": "UC123",
                "channelName": "Alpha",
                "channelUrl": "https://www.youtube.com/@alpha",
                "avatarUrl": "https://example.com/avatar.png",
                "service": "youtube"
              }
            ]
            """.trimIndent(),
            "json"
        )

        assertEquals(
            listOf(SubscribeRequest(
                channelId = "UC123",
                channelName = "Alpha",
                channelUrl = "https://www.youtube.com/@alpha",
                avatarUrl = "https://example.com/avatar.png",
                service = "youtube"
            )),
            subscriptions
        )
    }
}
