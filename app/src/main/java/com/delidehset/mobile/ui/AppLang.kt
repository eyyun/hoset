package com.delidehset.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The language of the app — the same choice as the site's, in one place.
 *
 * Every screen here is written in Turkish, so the app has always *been* in Turkish, no
 * matter what the site's own language setting said. This makes that a choice: the screens
 * read their text through [t] (see the `Text` in `ui/AppText.kt`), and the dictionary in
 * Translations.kt is what turns the Turkish they are written in into one of the site's
 * eight languages.
 *
 * The list is the site's own: `tr, en, de, es, fr, pt-br, ru, ja`. Picking one here sets
 * the site's language too — see `AppViewModel.setLang` — because to the person picking they
 * are the same setting.
 */
enum class AppLang(val code: String, val label: String) {
    TR("tr", "Türkçe"),
    EN("en", "English"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    FR("fr", "Français"),
    PT_BR("pt-br", "Português (BR)"),
    RU("ru", "Русский"),
    JA("ja", "日本語"),
    ;

    companion object {
        fun of(code: String?): AppLang = entries.firstOrNull { it.code.equals(code, true) } ?: TR
    }
}

val LocalAppLang = staticCompositionLocalOf { AppLang.TR }

/** The line, in the language the user picked. */
@Composable
fun t(text: String): String = translate(LocalAppLang.current, text)

/**
 * A line with a value inside it: `tf("Seste {0} kişi", voice.count)`.
 *
 * The value goes in after the wording is translated, so a room name or a user name — which
 * is data, not a translation — is never fed to the dictionary, and a sentence can be
 * re-ordered around its value without the call site knowing.
 */
@Composable
fun tf(template: String, vararg values: Any?): String = fill(t(template), *values)
