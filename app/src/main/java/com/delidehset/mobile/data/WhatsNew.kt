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
        "1.3.8" to listOf(
            "Profilde aşağı çekince takılan yenileme animasyonu düzeldi.",
            "Sohbetteki çevrimiçi yazıları küçük ikonlu sayılara dönüştü.",
            "Detaylı temalarda hareketli arka planı kapatabilir veya yavaşlatabilirsin; alt bar da tema renklerini alıyor.",
            "Topluluk ayar ikonu sadece yönetenlere görünüyor.",
        ),
        "1.3.7" to listOf(
            "Profiller artık bekletmeden anında açılıyor; kapak ve profil fotoğraflarını büyütüp indirebilirsin.",
            "Hoset Mod özellikleri geldi: sohbette kendini gizleme (hayalet mod), çift dokunuşla beğenme, video/görsel indirme ve gizli Truescore puanı.",
            "Sohbette kimlerin odada, kimlerin sitede çevrimiçi olduğunu ve bağlantı güçlerini görebilirsin.",
            "Detaylı temalar baştan aşağı yenilendi: yüzen alt bar, hareketli arka plan efektleri ve özel kart tasarımları.",
            "Uygulama simgelerine yeşil kedi (Delidehşet) eklendi, ayarlara simge önizlemeleri kondu.",
        ),
        "1.3.6" to listOf(
            "Oy butonları daha sade ve temiz hale getirildi.",
            "Topluluklardaki gönderi sayıları ile profildeki takipçi sayıları artık doğru görünüyor.",
            "Sayfa geçişlerinde kendi kendine çıkan yenileme halkası kaldırıldı, sadece aşağı çekince çıkıyor.",
        ),
        "1.3.5" to listOf(
            "Ayarlardan eski sürümleri indirebilir veya deneysel sürümleri açabilirsin.",
            "Gizli topluluklara katılma isteği gönderme ve moderatörler için topluluk ayarları eklendi.",
            "Site kuralları artık menüden direkt uygulama içinde açılıyor.",
            "Animasyonları sevmiyorsan ayarlardan hareketleri azaltabilirsin.",
        ),
        "1.3.4" to listOf(
            "Sohbetten başka ekrana geçsen bile bağlantın kopmuyor, geri döndüğünde kaldığın yerden devam ediyorsun.",
            "Ayarlardan galerindeki bir fotoğrafı kırpıp uygulama simgesi yapabilirsin.",
            "delidehset.com linkleri artık tarayıcı yerine direkt uygulamada açılıyor.",
            "Tema mağazasına basit ve detaylı tema seçenekleri eklendi.",
        ),
        "1.3.3" to listOf(
            "Güncelleme indirirken uygulamanın takılma sorunu çözüldü.",
            "Profilden bir yoruma tıkladığında direkt o yorumun olduğu yere kayıyor.",
        ),
        "1.3.2" to listOf(
            "Yorumlarda, akışta ve listelerde herkesin profil fotoğrafı görünüyor.",
            "Videolar artık siyah ekran yerine kendi önizleme karesiyle duruyor.",
            "Takipçi ve takip edilen listeleri uygulama içinde açılıyor.",
        ),
    )

    fun forVersion(version: String): List<String> = byVersion[version].orEmpty()
}
