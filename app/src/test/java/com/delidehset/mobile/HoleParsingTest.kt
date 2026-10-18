package com.delidehset.mobile

import com.delidehset.mobile.data.HtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The community table, the two follower counts, the settings page and the lock screen.
 *
 * Every snippet here is copied from what delidehset.com actually answered on 18 Oct 2026 —
 * including the admin variant of the table, which has an extra column and is the one the
 * app's own admin account sees.
 */
class HoleParsingTest {

    /**
     * The admin table. The first cell is the row number, which the old reader took for
     * the post count because it was the first cell that parsed as a number.
     */
    private val adminTable = """
        <table class="table">
          <thead><tr><th>#</th><th>Hole Name</th><th>Posts</th><th>Created on</th><th>Stealth</th><th>Public Use</th><th>Admin</th></tr></thead>
          <tbody>
            <tr>
              <td>1</td>
              <td><a href="/h/delidehset" hx-boost="true">/h/delidehset</a></td>
              <td><a href="/h/delidehset" hx-boost="true">0</a></td>
              <td>17 Oct 2026</td><td>No</td><td>No</td>
              <td class="text-nowrap"><button onclick="adminBanHole('delidehset')">Ban</button></td>
            </tr>
            <tr>
              <td>2</td>
              <td><a href="/h/general" hx-boost="true">/h/general</a></td>
              <td><a href="/h/general" hx-boost="true">1.2k</a></td>
              <td>29 Sep 2026</td><td>No</td><td>Yes</td>
              <td class="text-nowrap"><button onclick="adminBanHole('general')">Ban</button></td>
            </tr>
          </tbody>
        </table>
    """.trimIndent()

    @Test
    fun `the row number is not a post count`() {
        val holes = HtmlParser.parseHoles(adminTable)
        assertEquals(2, holes.size)
        assertEquals("delidehset", holes[0].name)
        assertEquals("the index column was read as posts", 0, holes[0].postCount)
        assertEquals("general", holes[1].name)
        assertEquals("1.2k should read as 1200", 1200, holes[1].postCount)
        assertEquals("29 Sep 2026", holes[1].createdOn)
        assertEquals("Yes", holes[1].publicUse)
    }

    /** A table without a `<thead>` still cannot mistake the index for the count. */
    private val headlessTable = """
        <table><tbody>
          <tr><td>7</td><td><a href="/h/meta">/h/meta</a></td><td><a href="/h/meta">13</a></td></tr>
        </tbody></table>
    """.trimIndent()

    @Test
    fun `a table with no headings reads the linked count`() {
        val hole = HtmlParser.parseHoles(headlessTable).single()
        assertEquals(13, hole.postCount)
        assertEquals("meta", hole.name)
    }

    @Test
    fun `the follower heading carries the number the header gets wrong`() {
        assertEquals(1, HtmlParser.parseRelationTotal("<div id=\"main-content-col\"><h3 class=\"mt-3\">cronus Follows (1)</h3></div>"))
        assertEquals(12, HtmlParser.parseRelationTotal("<div id=\"main-content-col\"><h3>casdil&#x27;s Followers (12)</h3></div>"))
        assertNull("a post page is not a relation page", HtmlParser.parseRelationTotal("<div id=\"main-content-col\"><h3>merhaba</h3></div>"))
    }

    @Test
    fun `the settings page gives the three editable values and the permission`() {
        val page = """
            <html><head><title>Settings for /h/general - Delidehset</title></head><body>
            <div id="main-content-col">
              <input class="form-control" name="display_name" placeholder="/h/general">
              <img loading="lazy" alt="Hole logo" src="/assets/images/logo.webp" />
              <form onsubmit="return saveHoleContentFilters(event, this, 'general')">
                <textarea id="hole-blocked-ip-ranges" name="blocked_ip_ranges" rows="4">192.0.2.5
198.51.100.0/24</textarea>
                <textarea id="hole-blocked-words" name="blocked_words" rows="4">spam
reklam</textarea>
              </form>
            </div></body></html>
        """.trimIndent()
        val settings = HtmlParser.parseHoleSettings(page, "general")
        assertNotNull(settings)
        settings!!
        assertTrue("the form is an editor, so this account may save", settings.canEdit)
        assertEquals("", settings.displayName)
        assertEquals("192.0.2.5\n198.51.100.0/24", settings.blockedIps)
        assertEquals("spam\nreklam", settings.blockedWords)
        assertEquals("/assets/images/logo.webp", settings.logoUrl)
    }

    @Test
    fun `a member sees no settings form and no permission`() {
        val page = """
            <html><head><title>Settings for /h/general - Delidehset</title></head><body>
            <div id="main-content-col"><h4>Content filters</h4></div></body></html>
        """.trimIndent()
        val settings = HtmlParser.parseHoleSettings(page, "general")
        assertNotNull(settings)
        assertFalse(settings!!.canEdit)
    }

    @Test
    fun `the private community lock screen is read instead of an error`() {
        val page = """
            <html><body><div id="main-content-col">
              <div class="container text-center mt-5">
                <h1><i class="fas fa-lock"></i> /h/yavsarlar is private</h1>
                <p class="text-muted mt-3" style="max-width:640px;margin:0 auto">Sadece akrabalar alınacaktır</p>
                <button type="button" class="btn btn-primary mt-4" onclick="openJoinModal('yavsarlar')">
                    Request to join
                </button>
              </div>
            </div></body></html>
        """.trimIndent()
        val lock = HtmlParser.parseHoleLock(page)
        assertNotNull(lock)
        assertTrue(lock!!.locked)
        assertTrue(lock.canRequest)
        assertEquals("Sadece akrabalar alınacaktır", lock.reason)
    }

    @Test
    fun `an ordinary community page is not a lock screen`() {
        assertNull(HtmlParser.parseHoleLock("<div id=\"main-content-col\"><h1>/h/general</h1></div>"))
    }

    @Test
    fun `the hole page's settings link is the membership permission`() {
        val manager = HtmlParser.parseHoleState("<div id=\"main-content-col\"><a href=\"/h/general/settings\">settings</a></div>")
        assertTrue(manager.canManage)

        val member = HtmlParser.parseHoleState("<div id=\"main-content-col\"><a href=\"/h/general/mods\">mods</a></div>")
        assertFalse(member.canManage)
    }
}
