package com.delidehset.mobile

import com.delidehset.mobile.data.HtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settings pages are built by the same templates as the snippets below (the markup
 * was copied from the live pages, with the values replaced). They are worth pinning
 * down because the two `<select>`s carry no `name` attribute — the site's JavaScript
 * reads them by id — so a generic form reader would silently drop them.
 */
class SettingsParserTest {

    private val personalPage = """
        <html><body>
        <nav><ul class="nav settings-nav">
            <li class="nav-item"><a class="nav-link active" href="/settings/personal">Personal</a></li>
            <li class="nav-item"><a class="nav-link" href="/settings/security">Security</a></li>
        </ul></nav>
        <form id="profile-settings" class="mt-3" onsubmit="return saveSettings(event, this)">
            <input type="hidden" name="__RequestVerificationToken" value="tok123">
            <div class="card p-3 mb-3">
                <h5>Site Language</h5>
                <select id="language-select" class="form-control mr-2">
                    <option value="en">English</option>
                    <option value="es">Espa&#xF1;ol</option>
                    <option value="tr" selected="selected">T&#xFC;rk&#xE7;e</option>
                </select>
            </div>
            <div class="card p-3 mb-3">
                <h5>Theme</h5>
                <select id="theme-select" class="form-control" onchange="applyTheme(this.value)">
                    <option value="light">light</option>
                    <option value="midnight" selected="selected">midnight</option>
                </select>
                <div id="themecolor-pickr" data-initial="805ad5" style="min-width:120px"></div>
            </div>
            <!--
                The live page nests the picture/banner forms inside this one. HTML parsing
                closes the outer form at the first inner `</form>`, so everything below is
                really outside `#profile-settings` — the parser must not rely on the form.
            -->
            <div class="card p-3 mb-3">
                <form action="/settings/images/profile" method="post" enctype="multipart/form-data">
                    <input type="hidden" name="__RequestVerificationToken" value="tok123">
                    <input type="file" name="profile">
                </form>
            </div>
            <div class="card p-3 mb-3">
                <h5>Bio</h5>
                <textarea class="form-control" name="bio" rows="4" maxlength="500">merhaba d&#xFC;nya</textarea>
            </div>
            <div class="card p-3 mb-3">
                <h5>Pronouns</h5>
                <input type="text" style="max-width:200px" name="pronouns" value="o/onun" maxlength="15">
            </div>
        </form>
        </body></html>
    """.trimIndent()

    private val securityPage = """
        <html><body>
        <form onsubmit="return changePassword(event, this)">
            <input type="password" name="old_password" required>
            <input type="password" name="new_password" minlength="8" required>
            <input type="password" name="new_password_confirm" minlength="8" required>
        </form>
        <form onsubmit="return changeEmail(event, this)">
            <input type="password" name="password" required>
            <input type="email" name="email" value="kullanici@example.com">
        </form>
        </body></html>
    """.trimIndent()

    @Test
    fun `personal settings keep the values the server rendered`() {
        val s = HtmlParser.parsePersonalSettings(personalPage)
        assertEquals("bio okunmalı", "merhaba dünya", s.bio)
        assertEquals("zamirler okunmalı", "o/onun", s.pronouns)
        assertEquals("seçili dil okunmalı", "tr", s.language)
        assertEquals("seçili tema okunmalı", "midnight", s.theme)
        assertEquals("tema rengi okunmalı", "805ad5", s.themeColor)
    }

    @Test
    fun `language and theme options come from the real selects`() {
        val s = HtmlParser.parsePersonalSettings(personalPage)
        assertEquals(listOf("en", "es", "tr"), s.languageOptions.map { it.first })
        assertEquals("Türkçe", s.languageOptions.last().second)
        assertEquals(listOf("light", "midnight"), s.themeOptions.map { it.first })
    }

    @Test
    fun `a page with no selected option falls back to the first one`() {
        val html = personalPage.replace(" selected=\"selected\"", "")
        val s = HtmlParser.parsePersonalSettings(html)
        assertEquals("en", s.language)
        assertEquals("light", s.theme)
    }

    @Test
    fun `a nested upload form does not hide the fields below it`() {
        // Regression: the bio sits after a nested <form> on the real page, and reading it
        // through `form#profile-settings` used to return an empty string — which the
        // settings screen would then save back over the real value.
        val s = HtmlParser.parsePersonalSettings(personalPage)
        assertEquals("merhaba dünya", s.bio)
        assertEquals("o/onun", s.pronouns)
    }

    @Test
    fun `missing settings page yields blanks instead of throwing`() {
        val s = HtmlParser.parsePersonalSettings("<html><body><p>login</p></body></html>")
        assertEquals("", s.bio)
        assertTrue("seçenek listesi boş olmalı", s.languageOptions.isEmpty())
    }

    @Test
    fun `security settings read the email field`() {
        val s = HtmlParser.parseSecuritySettings(securityPage)
        assertEquals("kullanici@example.com", s.email)
        assertFalse("setup formu yokken 2FA sunulmamalı", s.twoFactorAvailable)
    }

    @Test
    fun `the always present but hidden two factor block does not claim a state`() {
        // The live page ships this block with `d-none` even when 2FA was never set up,
        // so it must only light up the "feature exists" flag.
        val html = securityPage + """
            <form id="twofa-setup" onsubmit="return setup2Fa(event, this)"></form>
            <div id="twofa-qr" class="mt-3 d-none">
              <div id="twofa-qr-img"></div>
              <form onsubmit="return verify2Fa(event, this)">
                <input type="text" name="code" maxlength="6" required>
              </form>
            </div>
        """.trimIndent()
        val s = HtmlParser.parseSecuritySettings(html)
        assertTrue("2FA kurulumu sunuluyor olmalı", s.twoFactorAvailable)
    }
}
