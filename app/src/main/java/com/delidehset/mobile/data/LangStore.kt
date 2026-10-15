package com.delidehset.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.langDataStore by preferencesDataStore(name = "delidehset_language")

/**
 * The language of the app itself.
 *
 * The site's own language setting (Ayarlar → Kişisel → Dil) only ever changed the site:
 * the app around it stayed in Turkish whatever was picked. This is where the app's own
 * language lives, kept apart from the session on purpose — changing it must not touch
 * anything else, and it survives signing out.
 */
class LangStore(private val context: Context) {

    private val key = stringPreferencesKey("app_language")

    /** The chosen language code, or null while the user has never picked one. */
    val language: Flow<String?> = context.langDataStore.data.map { it[key] }

    suspend fun save(code: String) {
        context.langDataStore.edit { it[key] = code }
    }
}
