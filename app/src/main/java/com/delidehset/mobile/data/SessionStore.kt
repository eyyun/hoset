package com.delidehset.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "delidehset_session")

/**
 * Small cache of who the viewer is, so the drawer/nav can render immediately on
 * launch while the network confirms the real state.
 */
class SessionStore(private val context: Context) : ViewerCache {

    private object Keys {
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val USERNAME = stringPreferencesKey("username")
        val HREF = stringPreferencesKey("href")
        val AVATAR = stringPreferencesKey("avatar")
    }

    val viewer: Flow<Viewer> = context.dataStore.data.map { p ->
        Viewer(
            isSignedIn = p[Keys.SIGNED_IN] ?: false,
            username = p[Keys.USERNAME],
            href = p[Keys.HREF],
            avatarUrl = p[Keys.AVATAR],
        )
    }

    override suspend fun save(viewer: Viewer) {
        context.dataStore.edit { p ->
            p[Keys.SIGNED_IN] = viewer.isSignedIn
            if (viewer.username != null) p[Keys.USERNAME] = viewer.username else p.remove(Keys.USERNAME)
            if (viewer.href != null) p[Keys.HREF] = viewer.href else p.remove(Keys.HREF)
            if (viewer.avatarUrl != null) p[Keys.AVATAR] = viewer.avatarUrl else p.remove(Keys.AVATAR)
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
