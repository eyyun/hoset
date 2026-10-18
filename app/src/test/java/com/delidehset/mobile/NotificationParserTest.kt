package com.delidehset.mobile

import com.delidehset.mobile.data.HtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `/notifications` as the site actually sent it (delidehset.har, 18 Oct 2026): one unread
 * row on a post, plus the header bell that says how many are waiting.
 *
 * The page is the only source the app has for this — the site offers no JSON endpoint for
 * notifications — so the parser is pinned to the markup rather than to a nicer shape.
 */
class NotificationParserTest {

    private val page = """
        <html><body>
        <nav>
          <a class="nav-link position-relative" href="/notifications" title="Bildirimler">
            <span class="notif-count unbreakable ml-1" style="padding-left:4.5px">1</span>
          </a>
        </nav>
        <div id="main-content-col">
          <h2 class="mt-3">Notifications</h2>
          <button type="button" class="btn btn-primary mt-3"
                  onclick="postToastReload(this,'/clear')">Mark all notifications as read</button>
          <div class="mt-3">
            <ul class="list-group">
              <li class="list-group-item">
                <div class="d-flex justify-content-between flex-wrap">
                  <div>
                    <span class="badge bg-danger me-2">new</span>
                    <span class="text-muted small">13 saat önce</span>
                  </div>
                  <a class="small" href="/post/01a1319a-fe61-7a22-a5e2-985f8903e720/comment/01a14969-a2c0-7050-8367-83a67a0fbae6">context</a>
                </div>
                <div class="mt-1">
                  <div class="small text-muted mb-1">on: <a href="/post/01a1319a-fe61-7a22-a5e2-985f8903e720">Delidehşete hoşgeldiniz</a></div>
                  <div class="comment-body"><p>as</p></div>
                </div>
              </li>
              <li class="list-group-item">
                <div class="d-flex justify-content-between flex-wrap">
                  <div>
                    <span class="text-muted small">2 gün önce</span>
                  </div>
                  <a class="small" href="/post/01a1319a-fe61-7a22-a5e2-985f8903e720/comment/01a14969-a2c0-7050-8367-000000000000">context</a>
                </div>
                <div class="mt-1">
                  <div class="small text-muted mb-1">on: <a href="/post/01a1319a-fe61-7a22-a5e2-985f8903e720">Delidehşete hoşgeldiniz</a></div>
                  <div class="comment-body"><p>ikinci</p></div>
                </div>
              </li>
            </ul>
          </div>
        </div>
        </body></html>
    """.trimIndent()

    @Test
    fun `the rows come out in the order the page draws them`() {
        val parsed = HtmlParser.parseNotifications(page)
        assertEquals(2, parsed.items.size)
        assertEquals("/post/01a1319a-fe61-7a22-a5e2-985f8903e720/comment/01a14969-a2c0-7050-8367-83a67a0fbae6", parsed.items[0].href)
        assertEquals("Delidehşete hoşgeldiniz", parsed.items[0].postTitle)
        assertEquals("/post/01a1319a-fe61-7a22-a5e2-985f8903e720", parsed.items[0].postHref)
        assertEquals("as", parsed.items[0].text)
        assertEquals("13 saat önce", parsed.items[0].ago)
    }

    @Test
    fun `only the row with the new badge is unread`() {
        val parsed = HtmlParser.parseNotifications(page)
        assertTrue("ilk satır okunmamış", parsed.items[0].unread)
        assertFalse("ikinci satır okunmuş", parsed.items[1].unread)
    }

    @Test
    fun `the bell's number is the count, not the number of rows on this page`() {
        // The page paginates; the bell does not. A count of 7 with 2 rows means 5 older
        // ones are waiting on another page, and the badge must say 7.
        val paged = page.replace(">1</span>", ">7</span>")
        assertEquals(7, HtmlParser.parseNotifications(paged).unreadCount)
        // No bell in the markup (a partial response) falls back to the rows themselves.
        val noBell = page.replace(Regex("<span class=\"notif-count[^>]*>[^<]*</span>"), "")
        assertEquals(1, HtmlParser.parseNotifications(noBell).unreadCount)
    }

    @Test
    fun `an empty page is empty, not an error`() {
        val parsed = HtmlParser.parseNotifications(
            "<html><body><div id=\"main-content-col\"><h2>Notifications</h2><p>No notifications yet.</p></div></body></html>"
        )
        assertTrue(parsed.items.isEmpty())
        assertEquals(0, parsed.unreadCount)
    }

    @Test
    fun `the bell writes nine plus once it gets big`() {
        val capped = page.replace(">1</span>", ">9+</span>")
        assertEquals(9, HtmlParser.parseNotifications(capped).unreadCount)
    }
}
