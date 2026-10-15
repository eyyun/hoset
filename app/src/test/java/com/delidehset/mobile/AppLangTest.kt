package com.delidehset.mobile

import com.delidehset.mobile.ui.AppLang
import com.delidehset.mobile.ui.Phrase
import com.delidehset.mobile.ui.phrases
import com.delidehset.mobile.ui.translate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app is written in Turkish and translated by a dictionary, which makes one failure mode
 * possible and very annoying: a line that never made it into the dictionary stays Turkish in
 * every language. That is what this file is for — it reads the app's own sources and insists
 * that nothing Turkish is left unaccounted for.
 */
class AppLangTest {

    @Test
    fun `every phrase is written in all eight languages`() {
        assertTrue("sözlük beklenmedik kadar küçük: ${phrases.size}", phrases.size >= 150)
        for (phrase in phrases) {
            for (lang in AppLang.entries) {
                assertTrue(
                    "${lang.code}: \"${phrase.tr}\" boş",
                    phrase.inLang(lang).isNotBlank(),
                )
            }
        }
    }

    @Test
    fun `no Turkish line is written twice`() {
        // A repeated source would silently shadow the earlier row, so a fix applied to the
        // first one would have no effect at all.
        val seen = HashMap<String, Phrase>()
        for (phrase in phrases) {
            val previous = seen.put(phrase.tr, phrase)
            if (previous != null) {
                assertEquals("aynı satır iki kez yazılmış: \"${phrase.tr}\"", previous.inLang(AppLang.EN), phrase.inLang(AppLang.EN))
            }
        }
    }

    @Test
    fun `every language is different from the others`() {
        for (lang in AppLang.entries) {
            // Turkish is the source, so of course it matches itself; the others are the
            // ones that have to differ — a handful of shared words is normal ("profil"
            // is "profil" in French), a whole language's worth is a copy.
            if (lang == AppLang.TR) continue
            val same = phrases.count { it.inLang(lang) == it.tr }
            assertTrue("${lang.code}: $same satır çevrilmemiş", same <= 6)
        }
        // A table that was copied instead of translated is not a translation.
        val tables = AppLang.entries.associateWith { lang -> phrases.map { it.inLang(lang) } }
        for (a in AppLang.entries) {
            for (b in AppLang.entries) {
                if (a >= b) continue
                val shared = tables.getValue(a).filterIndexed { index, line -> line == tables.getValue(b)[index] }.size
                assertTrue("${a.code} ve ${b.code} aynı: $shared satır", shared < phrases.size / 2)
            }
        }
    }

    @Test
    fun `the lines the screens rely on read right`() {
        assertEquals("Settings", translate(AppLang.EN, "Ayarlar"))
        assertEquals("Sohbet", translate(AppLang.TR, "Sohbet"))
        assertEquals("Chat", translate(AppLang.DE, "Sohbet"))
        assertEquals("Atrás", translate(AppLang.ES, "Geri"))
        assertEquals("Connexion", translate(AppLang.FR, "Giriş"))
        assertEquals("Configurações", translate(AppLang.PT_BR, "Ayarlar"))
        assertEquals("Настройки", translate(AppLang.RU, "Ayarlar"))
        assertEquals("設定", translate(AppLang.JA, "Ayarlar"))
    }

    @Test
    fun `every offered language is translated, not just the first two`() {
        for (lang in AppLang.entries) {
            if (lang == AppLang.TR) continue
            for (source in listOf("Sohbet", "Ayarlar", "Gönderi", "Profil fotoğrafı", "Sese katıl", "Çevrimiçi")) {
                assertNotEquals("${lang.code}: $source çevrilmemiş", source, translate(lang, source))
            }
        }
    }

    @Test
    fun `text that is not the app's own wording is left alone`() {
        for (lang in AppLang.entries) {
            assertEquals("general", translate(lang, "general"))
            assertEquals("Bağlantı koptu.", translate(lang, "Bağlantı koptu."))
            assertEquals("admin", translate(lang, "admin"))
        }
    }

    /**
     * The complaint that started this: screens that still said Turkish after switching to
     * another language. Every user-facing line in the app's own sources has to be either in
     * the dictionary or on the short list below of things that are not Turkish to begin
     * with — a room name, a key, an icon description, an English word the app shows as is.
     */
    @Test
    fun `no Turkish line is missing from the dictionary`() {
        val missing = ArrayList<String>()
        for (file in sources()) {
            for (line in literalsIn(file.readText())) {
                if (!looksTurkish(line)) continue
                if (IGNORED.any { it == line }) continue
                if (translate(AppLang.EN, line) == line && !phrases.any { it.tr == line }) missing += line
            }
        }
        assertTrue(
            "sözlükte olmayan Türkçe satır(lar):\n" + missing.distinct().sorted().joinToString("\n"),
            missing.isEmpty(),
        )
    }

    /**
     * The string literals of a source file, comments left out.
     *
     * Written by hand rather than as a regular expression, because the pattern for "a
     * quoted string, with escapes" makes java.util.regex recurse once per character and
     * a single file is enough to blow the test JVM's stack.
     */
    private fun literalsIn(text: String): List<String> {
        val found = ArrayList<String>()
        for (raw in text.lineSequence()) {
            if (raw.contains("\"\"\"")) continue // a raw string is not a line of the UI
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                if (c == '/' && i + 1 < raw.length && raw[i + 1] == '/') break // comment
                if (c != '"') { i++; continue }
                val literal = StringBuilder()
                i++
                while (i < raw.length && raw[i] != '"') {
                    if (raw[i] == '\\' && i + 1 < raw.length) {
                        literal.append(raw[i + 1])
                        i += 2
                        continue
                    }
                    literal.append(raw[i])
                    i++
                }
                found += literal.toString()
                i++
            }
        }
        return found
    }

    private fun sources(): List<File> {
        val roots = listOf(
            File("src/main/java/com/delidehset/mobile"),
            File("app/src/main/java/com/delidehset/mobile"),
        )
        val root = roots.firstOrNull { it.isDirectory }
        assertTrue("kaynak dizini bulunamadı", root != null)
        return root!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name !in TRANSLATION_FILES }
            .toList()
    }

    private fun looksTurkish(line: String): Boolean {
        if (line.isBlank() || line.length < 2) return false
        if (line.startsWith("http") || line.contains("%s") || line.contains("&")) return false
        // A selector, a path, a key: never something a person reads.
        if (line.startsWith("#") || line.startsWith("/") || line.startsWith(".")) return false
        if (line.startsWith("profile") || line.startsWith("post") || line.startsWith("user")) return false
        if (line.any { it in TURKISH_LETTERS }) return true
        return TURKISH_WORDS.any { line.contains(it) }
    }

    private companion object {
        val TRANSLATION_FILES = setOf("AppLang.kt", "AppText.kt", "Translations.kt")
        const val TURKISH_LETTERS = "çğıöşüÇĞİÖŞÜ"

        /** Turkish lines that happen to use no Turkish-only letter. */
        val TURKISH_WORDS = listOf(
            "Ara", "Dil", "Geri", "Gizli", "Hesap", "Kapat", "Kaydet", "Kayıtlı", "Mesaj",
            "Metin", "Oturum", "Rastgele", "Sebep", "Sil", "Seste", "Takip", "Tema",
            "Topluluk", "Uygulama", "Yanıtla", "Yorumlar", "Zamirler", "Zorunlu", "profil",
        )

        /** Literals that are not the app's own wording: names, keys, English, symbols. */
        val IGNORED = setOf(
            " ", "  ·  ", " · ", "—", "·", "\\n", "//", "/", "#", "@", "?", "; ", ": ", ".",
            "Bearer", "DKIM", "Feed", "Holes", "Home", "Hoset", "Host", "Leaderboard",
            "Moderation Log", "NSFW", "POST", "GET", "Stealth", "Stealth: ", "UI", "UTF-8",
            "Upvote", "Downvote", "Vote", "comment", "post", "user", "hole", "general",
            "At the bottom", "back to the newest", "current screen", "could not prepare image",
            "DEPRECATION", "DeliChat", "DeliUpload", "SetJavaScriptEnabled", "UNUSED_PARAMETER",
            "PERSONAL", "SECURITY", "no colour", "image/*", "default-profile-pic", "feed-effort",
            "ornek@eposta.com", "kullanıcı", "metin", "sil", "@\${profile.name}",
        )
    }
}
