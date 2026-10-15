<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo_hoset.png" width="140" alt="Hoset logosu">
</p>

<h1 align="center">Hoset</h1>

<p align="center"><strong>delidehset.com için resmi olmayan Android istemcisi.</strong></p>

Hoset, siteyi bir WebView'e sıkıştırmaz. Akış, gönderi sayfası, profil, topluluklar, ayarlar ve
sohbet odası (yazı ve ses) Jetpack Compose ile baştan yazılmıştır; uygulama siteyle, tarayıcının
yaptığı gibi konuşur — sayfaları indirir, ayrıştırır ve formları sitenin kendi uç noktalarına
gönderir.

## Öne çıkanlar

**Akış ve gönderiler**
Sıralama (popüler / en iyiler), topluluk akışı, effortpostlar, oy verme, yorumlar, gönderi
oluşturma — anonim ya da tripcode ile — silme, şikâyet.

**Sohbet odası**
Yazı ve ses bir arada. Oda, arkasındaki tek piksellik WebView'in websocket'i üzerinden çalışır
([ChatBridge](app/src/main/java/com/delidehset/mobile/data/ChatBridge.kt)); ekranda gördüğün ve
dokunduğun her şey Compose'dur. Yankı, mikrofon ve hoparlör davranışı bu katmanda yönetilir.

**Profil ve ayarlar**
Profil fotoğrafı ve banner yükleme (görsel gönderilmeden önce yeniden boyutlandırılır), bio,
zamirler, tema, şifre ve e-posta değişikliği, oturum yönetimi.

**Sekiz dil**
Sitenin kendi dil listesi — `tr, en, de, es, fr, pt-br, ru, ja` — tek bir seçiciden yönetilir ve
seçim hem uygulamayı hem siteyi değiştirir. Ayrıntısı aşağıda.

## Derleme

Gerekli olanlar: **JDK 17** ve **Android SDK** (proje `compileSdk 35`, `minSdk 26` kullanır).

1. SDK yolunu `local.properties` dosyasına yaz (bu dosya sürüm kontrolüne girmez):

   ```properties
   sdk.dir=C:\\Users\\<kullanıcı>\\android-sdk
   ```

2. Testleri çalıştır ve sürüm APK'sını üret:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

Gradle'ın kullanacağı JDK yolu makineye göre değiştiği için depoda tutulmaz; `JAVA_HOME` ya da
`~/.gradle/gradle.properties` içindeki `org.gradle.java.home` ile verilir. Sürüm derlemesi,
`keystore.properties` varsa onunla, yoksa hata ayıklama anahtarıyla imzalanır — imzalama
malzemesi depoya girmez.

## Dil sistemi

Ekranların tamamı Türkçe yazılmıştır; metinler [AppText.kt](app/src/main/java/com/delidehset/mobile/ui/AppText.kt)
içindeki `Text` üzerinden çizilir. O `Text`, her satırı [Translations.kt](app/src/main/java/com/delidehset/mobile/ui/Translations.kt)
sözlüğünden geçirir. Sözlükte bulunmayan bir satır olduğu gibi kalır; yani yeni bir metin eklemek
için ekranı değiştirmek gerekmez, sözlüğe bir satır yazmak yeter.

- Sözlük satır başına bir `Phrase`'tır: Türkçesi ve diğer yedi dildeki karşılığı. Sekiz dilin
  hepsi zorunlu olduğundan eksik bir çeviri **derleme hatası** verir; sessizce Türkçe kalmaz.
- İçinde değer geçen satırlar `{0}`, `{1}` yer tutucuları taşır: `tf("Seste {0} kişi", voice.count)`.
  Önce cümle çevrilir, değer sonra yerine konur — böylece bir kullanıcı adı ya da oda adı asla
  sözlüğe gönderilmez.
- Compose dışındaki katmanlar (veri katmanı, ViewModel'ler) `tr` ve `trf` kullanır; seçilen dil
  onlara `CurrentLang` üzerinden ulaşır ve hata mesajları da çevrilir.
- [AppLangTest](app/src/test/java/com/delidehset/mobile/AppLangTest.kt) uygulamanın kaynak
  dosyalarını tarar: Türkçe olduğu hâlde sözlükte bulunmayan tek bir satır bile kalırsa test
  başarısız olur.

## Proje yapısı

```
app/src/main/java/com/delidehset/mobile/
├── data/   HTTP, çerez, HTML ayrıştırma ve depo katmanı — siteyle konuşan her şey
├── vm/     ViewModel'ler; ekranların durumu burada tutulur
├── ui/     Compose ekranları, tema ve dil sistemi
└── MainActivity.kt   navigasyon ve oturum akışı
```

## Bilinen sınırlar

- Site beta sürecinde olduğu için sayfa biçimi değişebilir; ayrıştırıcı beklediğini bulamadığında
  bunu kullanıcıya söyler.
- İki adımlı doğrulama kurulumu QR kod okutmayı gerektirdiği için uygulamada yok, sitede yapılır.
- Profil fotoğrafının profil sayfasında görünmesi sitenin avatar servisine bağlıdır.

## Lisans

[MIT](LICENSE) — kodu dilediğin gibi kullanabilir, değiştirebilir ve dağıtabilirsin; tek koşul
telif ve lisans metnini korumak. Lisans yalnızca bu depodaki kodu kapsar: delidehset.com'un
adı, içeriği ve verisi üzerinde bir hak vermez.

## Sorumluluk notu

Bu istemci delidehset.com ile resmî bir bağlantısı olmayan, topluluk tarafından yazılmış bir
uygulamadır. Adı geçen sitenin kullanım koşulları kullanıcının sorumluluğundadır.
