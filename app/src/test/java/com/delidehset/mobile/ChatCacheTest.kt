package com.delidehset.mobile

import com.delidehset.mobile.data.ChatCache
import com.delidehset.mobile.data.ChatMessage
import com.delidehset.mobile.data.ChatState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The room is kept on disk so it opens with the conversation already in place. What is
 * worth pinning down is that a reread is faithful — the same author, body, colour and
 * reactions — and that a file that cannot be read is treated as "nothing cached" rather
 * than as a crash on launch.
 */
class ChatCacheTest {

    private fun message(id: String, author: String = "cronus", body: String = "<p>selam</p>") =
        ChatMessage(
            id = id,
            author = author,
            avatarUrl = "/avatar/$author.png",
            timeText = "21:0$id",
            bodyHtml = body,
            reactions = listOf("👍", "🔥"),
            authorColor = "#58A6FF",
        )

    @Test
    fun `a room survives the round trip`() {
        val state = ChatState(messages = listOf(message("1"), message("2", author = "Mianserin")))
        val back = ChatCache.decode(ChatCache.encode(state))
        assertEquals(state.messages, back.messages)
    }

    @Test
    fun `an empty room is not written`() {
        assertTrue(ChatCache.decode(ChatCache.encode(ChatState.Empty)).messages.isEmpty())
    }

    @Test
    fun `a broken file reads as nothing cached`() {
        assertTrue(ChatCache.decode("{ bu json değil").messages.isEmpty())
        assertTrue(ChatCache.decode("").messages.isEmpty())
        assertTrue(ChatCache.decode("""{"messages":[{"author":"x"}]}""").messages.isEmpty())
    }

    @Test
    fun `only the last eighty messages are kept`() {
        val many = (1..120).map { message(it.toString()) }
        val back = ChatCache.decode(ChatCache.encode(ChatState(messages = many)))
        assertEquals(80, back.messages.size)
        assertEquals("41", back.messages.first().id)
        assertEquals("120", back.messages.last().id)
    }
}
