package com.delidehset.mobile

import com.delidehset.mobile.ui.nav.DeepLinks
import com.delidehset.mobile.ui.nav.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The manifest hands every delidehset.com link to the app, so these paths decide where a
 * tap from a browser or a chat really lands. The interesting cases are the ones the site
 * itself writes: `/@name` for profiles, `#comment-<id>` for a comment, and the pages the
 * app has no native reader for, which must still open inside the app rather than fall
 * through to nothing.
 */
class DeepLinksTest {

    @Test
    fun postOpensThePostScreen() {
        assertEquals(
            Routes.post("bir-gonderinin-basligi"),
            DeepLinks.routeFor("https://delidehset.com/post/bir-gonderinin-basligi"),
        )
    }

    @Test
    fun postCarriesTheCommentItPointsAt() {
        // The site writes the anchor as `#comment-<uuid>`.
        assertEquals(
            Routes.post("bir-gonderi", "9f2c1a"),
            DeepLinks.routeFor("https://delidehset.com/post/bir-gonderi#comment-9f2c1a"),
        )
        // Some links carry it as a query parameter instead.
        assertEquals(
            Routes.post("bir-gonderi", "9f2c1a"),
            DeepLinks.routeFor("https://delidehset.com/post/bir-gonderi?comment=9f2c1a"),
        )
    }

    @Test
    fun postKeepsTheSlugOnly() {
        // A link to a comment's own page still opens the post, not `/post/<slug>/comment/…`.
        assertEquals(
            Routes.post("bir-gonderi", "9f2c1a"),
            DeepLinks.routeFor("https://delidehset.com/post/bir-gonderi/comment/9f2c1a"),
        )
    }

    @Test
    fun profileLinksOpenTheProfile() {
        assertEquals(Routes.user("eyyun"), DeepLinks.routeFor("https://delidehset.com/@eyyun"))
        // A tab of the profile is still the profile; the app has no per-tab route.
        assertEquals(Routes.user("eyyun"), DeepLinks.routeFor("https://delidehset.com/@eyyun/comments"))
        assertEquals(Routes.user("eyyun"), DeepLinks.routeFor("https://delidehset.com/u/eyyun"))
    }

    @Test
    fun communityLinksOpenItsFeed() {
        assertEquals("feed?hole=general", DeepLinks.routeFor("https://delidehset.com/h/general"))
    }

    @Test
    fun subdomainsAreTheSameSite() {
        assertEquals(
            Routes.user("eyyun"),
            DeepLinks.routeFor("https://www.delidehset.com/@eyyun"),
        )
    }

    @Test
    fun otherSitesAndSchemesAreNotOurs() {
        assertNull(DeepLinks.routeFor("https://example.com/@eyyun"))
        assertNull(DeepLinks.routeFor("https://delidehset.com.example.com/@eyyun"))
        assertNull(DeepLinks.routeFor("javascript:alert(1)"))
        assertNull(DeepLinks.routeFor("mailto:hoset@delidehset.com"))
        assertNull(DeepLinks.routeFor(""))
        assertNull(DeepLinks.routeFor("delidehset.com/@eyyun"))
    }

    @Test
    fun pagesWithoutANativeReaderKeepTheirPathQueryAndFragment() {
        assertEquals(
            "/search?q=kitap#sonuclar",
            DeepLinks.pathFor("https://delidehset.com/search?q=kitap#sonuclar"),
        )
        assertEquals("/settings/personal", DeepLinks.pathFor("https://delidehset.com/settings/personal"))
        assertEquals("/", DeepLinks.pathFor("https://delidehset.com"))
        assertNull(DeepLinks.pathFor("https://example.com/search?q=kitap"))
    }

    @Test
    fun nativeRoutesAreAlwaysUsableRoutes() {
        // Whatever a link produces must be a route the navigation graph knows, otherwise the
        // app would navigate nowhere at all.
        val links = listOf(
            "https://delidehset.com/post/bir-gonderi",
            "https://delidehset.com/@eyyun",
            "https://delidehset.com/h/general",
        )
        for (link in links) {
            val route = DeepLinks.routeFor(link)
            assertTrue("$link -> $route", route == null || route.startsWith("post?permalink=") ||
                route.startsWith("user?name=") || route.startsWith("feed?hole="))
        }
    }
}
