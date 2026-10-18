package com.delidehset.mobile

import com.delidehset.mobile.data.CommentNode
import com.delidehset.mobile.vm.adjustScore
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The score shown under a comment is the number people actually read, so it has to move
 * with the arrow. It used to stay put, which made a downvote look like it had not
 * registered at all — these cases pin the arithmetic down.
 */
class VoteScoreTest {

    private fun comment(id: String, score: Int, children: List<CommentNode> = emptyList()) =
        CommentNode(
            id = id,
            parentId = null,
            depth = 0,
            authorName = "cronus",
            authorHref = "/@cronus",
            authorAvatar = null,
            bodyHtml = "<p>$id</p>",
            score = score,
            viewerVote = 0,
            ago = null,
            isDeleted = false,
            children = children,
        )

    @Test
    fun `a downvote takes the score down and an upvote brings it back`() {
        val thread = listOf(comment("a", 5))
        assertEquals(4, thread.adjustScore("a", -1).first().score)
        assertEquals(6, thread.adjustScore("a", 1).first().score)
    }

    @Test
    fun `switching from up to down moves the score by two`() {
        // The arrow already shows +1; going straight to -1 is a two-point change.
        val thread = listOf(comment("a", 10))
        assertEquals(8, thread.adjustScore("a", -2).first().score)
    }

    @Test
    fun `a reply's score changes without touching its parent`() {
        val thread = listOf(comment("a", 5, children = listOf(comment("a1", 2))))
        val changed = thread.adjustScore("a1", -1)
        assertEquals(5, changed.first().score)
        assertEquals(1, changed.first().children.first().score)
    }

    @Test
    fun `a nested change reaches every level it has to`() {
        val thread = listOf(comment("a", 1, children = listOf(comment("a1", 3, children = listOf(comment("a2", 7))))))
        assertEquals(6, thread.adjustScore("a2", -1).first().children.first().children.first().score)
    }

    @Test
    fun `an unknown id changes nothing`() {
        val thread = listOf(comment("a", 4))
        assertEquals(thread, thread.adjustScore("yok", -1))
    }
}
