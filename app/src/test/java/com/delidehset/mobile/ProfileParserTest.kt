package com.delidehset.mobile

import com.delidehset.mobile.data.HtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The profile page marks every field with its own id, and the site's own JavaScript reads
 * exactly those ids. The snippet below is the live `/@admin` markup with the values
 * trimmed, so a markup change that would break the native profile shows up here first.
 */
class ProfileParserTest {

    private fun page(unsubHidden: Boolean = true) = """
        <html><body>
        <div id="main-content-col">
            <div id="profile--pfp">
                <a rel="noopener" href="/assets/images/default-profile-pic.webp" class="profile-pic-100-wrapper">
                    <img loading="lazy" src="/assets/images/default-profile-pic.webp" class="profile-pic profile-pic-100 mb-5">
                </a>
            </div>
            <div class="d-flex">
                <h3 class="font-weight-bolder my-0 mr-2" id="profile--name" style="color: #805ad5">admin</h3>
            </div>
            <div class="font-weight-bolder mb-2 text-left" id="profile--simphate">
                <a class="mx-1" href="/log?target_id=01a0ee5d-95e2-74ee-9a6c-8dce5a436183">Moderation History</a>
                <div class="font-weight-bolder mb-1">
                    <div class="my-2">
                        <a class="mr-1" href="/@admin/followers" hx-boost="true" id="profile--followers">0 followers</a> |
                        <a class="mx-1" href="/@admin/following" hx-boost="true" id="profile--following">follows 0 users</a>
                    </div>
                    <span id="profile-coins-amount">0</span>
                    <img loading="lazy" alt="coins" class="ml-1 mb-1 mr-2" height="20" src="/i/coins.webp">
                </div>
                <span id="profile--joined">Joined 29 Sep 2026</span>
            </div>
            <p class="text-muted" id="profile--bio">No bio...</p>
            <div id="profile--actionbtns">
                <div class="actionbtns">
                    <button type="button" id="button-sub" class="btn btn-primary ">Follow</button>
                    <button type="button" id="button-unsub" class="btn btn-secondary ${if (unsubHidden) "d-none" else ""}">Unfollow</button>
                </div>
            </div>
            <div class="mt-3" id="profile--info">
                <p id="profile--info--id">User ID: 01a0ee5d-95e2-74ee-9a6c-8dce5a436183</p>
            </div>
        </div>
        </body></html>
    """.trimIndent()

    @Test
    fun `join line is the joined field, not the bio placeholder`() {
        val profile = HtmlParser.parseProfile(page(), "admin")
        assertEquals("29 Sep 2026", profile.joinedAgo)
    }

    @Test
    fun `the site's own No bio placeholder is not shown as a bio`() {
        val profile = HtmlParser.parseProfile(page(), "admin")
        assertNull("placeholder biyografi sayılmamalı", profile.bio)
    }

    @Test
    fun `followers, following and coins are rendered as stats`() {
        val stats = HtmlParser.parseProfile(page(), "admin").stats.associate { it.key to it.value }
        assertEquals("0", stats["Takipçi"])
        assertEquals("0", stats["Takip"])
        // A zero balance is not worth a tile, so it is dropped rather than shown as "0".
        assertNull(stats["Coin"])
    }

    @Test
    fun `name, id and avatar come from the profile ids`() {
        val profile = HtmlParser.parseProfile(page(), "fallback")
        assertEquals("admin", profile.name)
        assertEquals("/@admin", profile.href)
        assertEquals("01a0ee5d-95e2-74ee-9a6c-8dce5a436183", profile.id)
        assertEquals("/assets/images/default-profile-pic.webp", profile.avatarUrl)
    }

    @Test
    fun `follow state follows the buttons the site toggles`() {
        assertFalse(HtmlParser.parseProfile(page(unsubHidden = true), "admin").isFollowing)
        assertTrue(HtmlParser.parseProfile(page(unsubHidden = false), "admin").isFollowing)
    }
}
