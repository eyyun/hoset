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
        "1.3.5" to listOf(
            "Deneysel sürümler ayarı geldi: açarsan ön sürümler sana da gelir.",
            "Sürüm geçmişi: eski bir sürümü seçip indirebilirsin.",
            "Hareketleri azalt ayarı ekran geçişlerini sadeleştirir.",
            "Kurallar menüden açılıyor; site kuralları uygulamanın kendi ekranında.",
        ),
        "1.3.4" to listOf(
            "Aşağı oy artık görünüyor: seçtiğin okun arkası dolu, puan da onun renginde.",
            "Sohbet ekrandan çıkınca bağlı kalıyor; dönüşte oda anında yerinde.",
            "Uygulama kapalıyken bile son sohbet mesajları ekranda duruyor.",
            "delidehset.com bağlantıları artık uygulamada açılıyor.",
            "Topluluk listesinde toplulukların kendi görselleri var.",
            "Ayarlardan kendi simgeni seçip kırpabilir, ana ekrana ekleyebilirsin.",
            "Tema Mağazası basit ve detaylı kitler olarak ikiye ayrıldı.",
        ),
        "1.3.3" to listOf(
            "Güncelleme kurulurken uygulama donmuyor; ilerleme görünür kalıyor.",
            "Kurulum biterken ekran artık yanıt vermiyor gibi durmuyor.",
        ),
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
