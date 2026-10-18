package com.delidehset.mobile

import com.delidehset.mobile.data.DeliCookieJar
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.data.DeliRepository
import com.delidehset.mobile.data.FeedSort
import com.delidehset.mobile.data.LoginResult
import com.delidehset.mobile.data.Viewer
import com.delidehset.mobile.data.ViewerCache
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * These tests run the app's own networking and parsing code (the same classes the
 * Android UI uses) against the live delidehset.com, on the JVM.
 *
 * They are the closest thing to end-to-end verification available without a device:
 * if the site changes its markup the parsers here break first.
 */
class LiveSiteTest {

    private class MemoryJar : DeliCookieJar {
        private val cookies = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies.removeAll { c -> cookies.any { it.name == c.name } }
            this.cookies.addAll(cookies)
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookies.filter { url.host.endsWith(it.domain.removePrefix(".")) }
        override fun clear() = cookies.clear()
        override fun hasSessionCookie() = cookies.any {
            val n = it.name
            n.contains("AspNetCore.Cookies", true) || n.contains("Identity.Application", true)
        }
        override fun importRawHeader(url: HttpUrl, header: String?) {
            header?.split(';')?.forEach { part ->
                val kv = part.trim().split('=', limit = 2)
                if (kv.size == 2) {
                    saveFromResponse(url, listOf(Cookie.Builder().name(kv[0]).value(kv[1]).domain(url.host).path("/").build()))
                }
            }
        }
        override fun exportRawHeader(url: HttpUrl): String =
            loadForRequest(url).joinToString("; ") { "${it.name}=${it.value}" }
    }

    private class NoopCache : ViewerCache {
        override suspend fun save(viewer: Viewer) = Unit
        override suspend fun clear() = Unit
    }

    private val jar = MemoryJar()
    private val http = DeliHttp(jar)
    private val repo = DeliRepository(http, jar, NoopCache())

    /* ----------------------------- feed ----------------------------- */

    @Test
    fun `feed api returns a parsable payload for every sort`() = runBlocking {
        for (sort in FeedSort.entries) {
            val feed = repo.feed(sort)
            assertEquals("istek sıralaması korunmalı", sort.key, feed.sort)
            for (post in feed.posts) {
                assertTrue("post id boş olmamalı", post.id.isNotBlank())
                assertTrue("permalink /h/ ile başlamalı: ${post.permalink}", post.permalink.startsWith("/h/"))
                assertTrue("başlık çözülmeli", post.titleHtml.isNotBlank())
            }
        }
    }

    @Test
    fun `home page bootstraps cookies and antiforgery token`() = runBlocking {
        val viewer = repo.bootstrap()
        // Anonymous browsing must work and must not claim a session.
        assertFalse("çıkış yapmadan oturum açık görünmemeli", viewer.isSignedIn)
        assertNotNull("antiforgery token yakalanmalı", http.currentAntiforgery())
        assertTrue("oturum çerezi olmamalı", !jar.hasSessionCookie())
    }

    /* --------------------------- submit form ------------------------- */

    @Test
    fun `submit form is discovered with the real field names`() = runBlocking {
        val form = repo.submitForm()
        assumeTrue("submit sayfası erişilebilir olmalı", form != null)
        form!!

        assertEquals("gönderi gövdesi alanı", "body", form.bodyField)
        assertEquals("başlık alanı", "title", form.titleField)
        assertTrue("hole alanı formda olmalı", form.fields.any { it.name == "hole" })
        assertTrue("topluluk listesi gelmeli", form.holeOptions.any { it.first == "general" })
    }

    /* ---------------------------- post page -------------------------- */

    @Test
    fun `post page and comments parse from real html`() = runBlocking {
        val feed = repo.feed(FeedSort.NEW)
        val first = feed.posts.firstOrNull()
        assumeTrue("sitede en az bir gönderi olmalı", first != null)

        val page = repo.post(first!!.permalink)
        assertEquals("permalink korunmalı", first.permalink, page.post.permalink)
        assertEquals("gönderi kimliği", first.id, page.post.id)
        assertTrue("başlık çözülmeli", page.post.title.isNotBlank())
        assertTrue("gövde çözülmeli", page.post.bodyHtml.isNotBlank())
        assertEquals("hole adı", first.hole, page.post.hole)
        // Comments are embedded as JSON; an empty list is legitimate for a new site.
        assertNotNull(page.comments)
    }

    @Test
    fun `holes and users pages parse`() = runBlocking {
        val holes = repo.holes()
        assertTrue("en az bir topluluk olmalı", holes.isNotEmpty())
        assertTrue("topluluk adı boş olmamalı", holes.all { it.name.isNotBlank() })
        assertTrue("/h/general listelenmeli", holes.any { it.name == "general" })

        val users = repo.users()
        assertTrue("kayıtlı kullanıcı listesi gelmeli", users.isNotEmpty())
        assertTrue("kullanıcı adı boş olmamalı", users.all { it.name.isNotBlank() })
        assertTrue("admin kullanıcısı olmalı", users.any { it.name == "admin" })
    }

    /* ---------------------------- profile ---------------------------- */

    /**
     * Pins the profile field ids against the live page: `.text-muted` on that page is the
     * bio, so reading it as the join line used to print "Katılım: No bio...".
     */
    @Test
    fun `profile page yields a join date instead of the bio placeholder`() = runBlocking {
        val profile = repo.profile("admin")

        // The whole profile page, not the part of it that was read first: the counters as
        // numbers, the id, and the page's own tabs all come off the live page.
        assertNotNull("takipçi sayısı okunmadı", profile.followers)
        assertNotNull("takip sayısı okunmadı", profile.following)
        assertTrue("kullanıcı kimliği boş", profile.id.isNotBlank())
        assertTrue("profil sekmeleri okunmadı: ${profile.tabs}", profile.tabs.isNotEmpty())
        assertTrue(
            "sekmelerden biri aktif değil",
            profile.tabs.any { it.active },
        )
        assertEquals("profil adı korunmalı", "admin", profile.name)

        val joined = profile.joinedAgo
        assertNotNull("katılım satırı bulunmalı", joined)
        assertTrue("katılım bir tarih olmalı: $joined", joined!!.any { it.isDigit() })
        assertFalse(
            "biyografi yer tutucusu katılım olarak okunmamalı: $joined",
            joined.contains("bio", ignoreCase = true),
        )
        assertFalse("No bio... biyografi sayılmamalı", profile.bio?.contains("No bio...") == true)
        assertTrue("takipçi sayısı istatistik olarak gelmeli", profile.stats.any { it.key == "Takipçi" })
    }

    @Test
    fun `search returns rows for a query`() = runBlocking {
        // The site currently has next to no content, so only the round-trip is asserted.
        val rows = repo.searchPosts("delidehset")
        assertNotNull("arama sonucu null olmamalı", rows)
    }

    /* ------------------------------ auth ----------------------------- */

    /**
     * Exercises the whole login pipeline (GET the form, keep the antiforgery cookie,
     * POST username/password/redirect) without touching a real account: the username
     * below cannot exist.
     */
    @Test
    fun `login posts the form and reports failure for a non existent account`() = runBlocking {
        val result = repo.login("zz_deli_native_test_user_zz", "not-a-real-password-123")
        assertTrue("hayali hesap için başarı dönmemeli", result is LoginResult.Failure)
        val message = (result as LoginResult.Failure).message
        assertTrue("hata mesajı dolu olmalı", message.isNotBlank())
    }

    @Test
    fun `protected pages raise not signed in instead of crashing`() = runBlocking {
        val thrown = runCatching { repo.table("/leaderboard") }.exceptionOrNull()
        // Either the page loads (if the site ever makes it public) or we get the
        // explicit "sign in" signal; never a raw parse failure.
        assertTrue(
            "beklenmeyen hata: $thrown",
            thrown == null || thrown is com.delidehset.mobile.data.NotSignedInException,
        )
    }
}
