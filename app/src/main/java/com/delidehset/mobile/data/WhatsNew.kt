package com.delidehset.mobile.data

/**
 * The few lines each recent version is worth telling the person about.
 *
 * An update installs itself and then says nothing, so the only way to notice a fix was to
 * go looking for it. These are the app's own words, not the release notes: the notes on
 * GitHub are for someone deciding whether to download, and this is for someone who has
 * just been updated and wants to know what to try.
 */
object WhatsNew {

    /** Newest first; the keys are `BuildConfig.VERSION_NAME` shapes. */
    private val byVersion: Map<String, List<String>> = mapOf(
        "1.3.2" to listOf(
            "Profil fotoğrafları artık yorumlarda, gönderilerde ve listelerde görünüyor.",
            "Yorum, takipçi ve takip listelerindeki hatalar düzeltildi.",
            "Video gönderileri kendi karesini gösteriyor.",
            "Geri düğmesi her ekranda çalışıyor.",
            "Sohbet daha hızlı açılıyor.",
        ),
    )

    fun forVersion(version: String): List<String> = byVersion[version].orEmpty()
}
