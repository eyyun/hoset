package com.delidehset.mobile

import com.delidehset.mobile.data.ActionResult
import com.delidehset.mobile.data.DeliResponse
import com.delidehset.mobile.data.DiscoveredForm
import com.delidehset.mobile.data.FieldKind
import com.delidehset.mobile.data.FormField
import com.delidehset.mobile.data.HtmlParser
import com.delidehset.mobile.data.commentFields
import com.delidehset.mobile.data.interpretAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins down the two things that made comments look broken in the app: the request body
 * `/comment` expects, and the fact that the site reports failures inside a JSON body
 * while still answering `200 OK`.
 */
class CommentAndActionTest {

    /** The reply form the site renders, copied from the live post page. */
    private val replyForm = DiscoveredForm(
        action = "/comment",
        fields = listOf(
            FormField("__RequestVerificationToken", FieldKind.HIDDEN, "tok"),
            FormField("parent_fullname", FieldKind.HIDDEN, "p_11111111-2222-3333-4444-555555555555"),
            FormField("body", FieldKind.TEXTAREA, ""),
        ),
        bodyField = "body",
        titleField = null,
        isMultipart = false,
    )

    private fun fields(
        form: DiscoveredForm?,
        postId: String,
        body: String,
        parentId: String?,
        hole: String? = null,
    ) = commentFields(form, postId, body, parentId, hole)

    @Test
    fun `a top level comment keeps the post reference the server rendered`() {
        val out = fields(replyForm, "99999999-8888-7777-6666-555555555555", "merhaba", null)
        assertEquals(
            "p_11111111-2222-3333-4444-555555555555",
            out.first { it.first == "parent_fullname" }.second,
        )
        assertEquals("merhaba", out.first { it.first == "body" }.second)
        assertEquals("gizli alan korunmalı", "tok", out.first { it.first == "__RequestVerificationToken" }.second)
        assertTrue("uydurma post_id gönderilmemeli", out.none { it.first == "post_id" })
        assertTrue("uydurma hole gönderilmemeli", out.none { it.first == "hole" })
        assertTrue("uydurma parent_id gönderilmemeli", out.none { it.first == "parent_id" })
    }

    @Test
    fun `a reply targets the comment with the sites c_ prefix`() {
        val out = fields(replyForm, "99999999-8888-7777-6666-555555555555", "cevap", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertEquals(
            "c_aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            out.first { it.first == "parent_fullname" }.second,
        )
    }

    @Test
    fun `a build without a thread field still gets the conventional names`() {
        val legacy = replyForm.copy(
            fields = listOf(
                FormField("token", FieldKind.HIDDEN, "t"),
                FormField("body", FieldKind.TEXTAREA, ""),
            ),
        )
        val out = fields(
            legacy,
            "99999999-8888-7777-6666-555555555555",
            "yorum",
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            hole = "general",
        )
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", out.first { it.first == "parent_id" }.second)
        assertEquals("99999999-8888-7777-6666-555555555555", out.first { it.first == "post_id" }.second)
        assertEquals("general", out.first { it.first == "hole" }.second)
    }

    /* ----------------------------- actions ---------------------------- */

    private fun json(body: String, code: Int = 200): ActionResult =
        interpretAction(DeliResponse(code, null, "application/json; charset=utf-8", body), "Yorum")

    @Test
    fun `a 200 that carries an error is a failure`() {
        val res = json("""{"error":"Yorum çok kısa"}""")
        assertFalse("hata gövdesi başarı sayılmamalı", res.ok)
        assertEquals("Yorum çok kısa", res.message)
    }

    @Test
    fun `the cid the site returns means the comment was created`() {
        val res = json("""{"cid":"01a13703-e34a-7018-a5b8-5e0d1728187a","message":"Yorum eklendi"}""")
        assertTrue("cid başarı demek", res.ok)
        assertEquals("Yorum eklendi", res.message)
    }

    @Test
    fun `a plain success payload is a success`() {
        assertTrue(json("""{"ok":true}""").ok)
    }

    @Test
    fun `a redirect back to the login page asks for a session`() {
        val res = interpretAction(DeliResponse(302, "/login?ReturnUrl=%2Fh%2Fgeneral", null, ""), "Yorum")
        assertFalse(res.ok)
        assertTrue("mesaj oturum istemeli: ${res.message}", res.message!!.contains("oturum"))
    }

    @Test
    fun `an html validation page surfaces the visible message`() {
        val html = """<div class="alert-danger"><li>Yorum gönderilemedi.</li></div>"""
        val res = interpretAction(DeliResponse(200, null, "text/html", html), "Yorum")
        assertFalse(res.ok)
        assertEquals("Yorum gönderilemedi.", res.message)
    }

    @Test
    fun `an empty 204 is a success`() {
        val res = interpretAction(DeliResponse(204, null, null, ""), "Oy")
        assertTrue(res.ok)
    }

    /* ----------------------- comment rendering ------------------------ */

    @Test
    fun `comment authors come from the flat author field the site sends`() {
        val html = """
            <html><body>
            <script id="replies-data" type="application/json">
            [{"id":"01a13703-e34a-7018-a5b8-5e0d1728187a","level":1,"score":1,"voted":1,
              "author":"cronus","author_href":"/@cronus","ago":"3 minutes ago","deleted":false,
              "body_html":"\u003Cp\u003Esa\u003C/p\u003E","children":[]}]
            </script>
            </body></html>
        """.trimIndent()

        val comments = HtmlParser.parseComments(html)
        assertEquals("bir yorum okunmalı", 1, comments.size)
        val c = comments.first()
        assertEquals("yazar adı dolu olmalı", "cronus", c.authorName)
        assertEquals("/@cronus", c.authorHref)
        assertEquals("şu anki oy durumu", 1, c.viewerVote)
        assertEquals("sa", HtmlParser.stripTags(c.bodyHtml))
        assertFalse(c.isDeleted)
    }
}
