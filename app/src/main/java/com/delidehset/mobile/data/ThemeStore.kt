package com.delidehset.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "delidehset_theme")

/**
 * The look of the app itself.
 *
 * The site has its own theme setting (Ayarlar → Kişisel → Tema), and it changes the site:
 * the app around it stayed purple whatever was picked there. This is the app's own choice,
 * kept on the device like the language, so it holds with no account and no network.
 */
class ThemeStore(private val context: Context) {

    private val key = stringPreferencesKey("app_theme")

    /** The chosen palette id, or null while the user has never picked one. */
    val theme: Flow<String?> = context.themeDataStore.data.map { it[key] }

    suspend fun save(id: String) {
        context.themeDataStore.edit { it[key] = id }
    }
}
