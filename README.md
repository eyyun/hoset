<p align="center">
  <b>Türkçe</b> · <a href="README.en.md">English</a>
</p>

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo_hoset.png" width="140" alt="Hoset logosu">
</p>

<h1 align="center">Hoset</h1>

<p align="center">delidehset.com için Jetpack Compose ile geliştirilmiş gayriresmi Android istemcisi.</p>

<p align="center">
  <a href="https://github.com/eyyun/hoset/releases/latest">APK indir</a> ·
  <a href="LICENSE">MIT lisansı</a>
</p>

Hoset, siteyi basit bir WebView içine gömmek yerine doğrudan web uç noktaları ve HTML yapısıyla haberleşir. Akış, gönderiler, profil, topluluklar ve sesli/yazılı sohbet odası tamamen yerel (native) Compose arayüzüyle çalışır.

## Özellikler

- **Akış ve Gönderiler:** Popüler ve en iyiler sıralaması, topluluk akışları, effortpost filtreleme, oy verme, yorumlar, anonim veya tripcode ile gönderi oluşturma, silme ve şikayet bildirme.
- **Sohbet Odası (Yazı ve Ses):** Arka planda çalışan hafif bir WebSocket köprüsü ([ChatBridge](app/src/main/java/com/delidehset/mobile/data/ChatBridge.kt)) üzerinden sesli ve yazılı sohbet. Arayüz ve ses/mikrofon yönetimi tamamen yerel bileşenlerle kontrol edilir.
- **Profil ve Hesap Ayarları:** Profil fotoğrafı ve banner yükleme (otomatik boyutlandırma ile), biyografi, zamirler, tema seçimi, şifre/e-posta güncelleme ve aktif oturum yönetimi.
- **Çoklu Dil Desteği:** Sitenin desteklediği 8 dil (`tr, en, de, es, fr, pt-br, ru, ja`) uygulama içinden seçilebilir ve hem istemciyi hem site dilini senkronize eder.

## Kurulum ve Derleme

Gereksinimler: **JDK 17** ve **Android SDK** (`compileSdk 35`, `minSdk 26`).

1. Android SDK yolunu `local.properties` içine tanımlayın:

   ```properties
   sdk.dir=C:\\Users\\<kullanici>\\android-sdk
   ```

2. Testleri çalıştırıp release APK derlemek için:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

> Not: `keystore.properties` tanımlıysa release APK bu anahtarla, tanımlı değilse varsayılan debug anahtarıyla imzalanır.

## Çeviri ve Dil Sistemi

Arayüz metinleri varsayılan olarak Türkçe tanımlıdır ve [AppText.kt](app/src/main/java/com/delidehset/mobile/ui/AppText.kt) içindeki bileşenler üzerinden [Translations.kt](app/src/main/java/com/delidehset/mobile/ui/Translations.kt) sözlüğüne bağlanır.

- Sözlükteki her kayıt (`Phrase`), Türkçe kaynak metin ve diğer 7 dildeki karşılıklarını içerir. Eksik bir dil çevirisi derleme aşamasında hata verir.
- Dinamik değer içeren metinlerde `{0}`, `{1}` gibi yer tutucular kullanılır (`tf("Seste {0} kişi", voice.count)`). Önce kalıp çevrilir, parametreler ardından yerleştirilir.
- [AppLangTest](app/src/test/java/com/delidehset/mobile/AppLangTest.kt), kod tabanındaki dizgileri tarayarak sözlükte karşılığı olmayan Türkçe metin kaldığında testi düşürür.

## Proje Yapısı

```
app/src/main/java/com/delidehset/mobile/
├── data/           HTTP istekleri, oturum/çerezler, HTML ayrıştırma ve veri katmanı
├── vm/             ViewModel sınıfları ve UI durum yönetimi
├── ui/             Compose ekranları, tema ve çeviri bileşenleri
└── MainActivity.kt Navigasyon ve temel oturum akışı
```

## Bilinen Durumlar

- Sitenin HTML yapısında yapılabilecek değişiklikler ayrıştırıcıyı (parser) etkileyebilir.
- İki adımlı doğrulama (2FA) ilk kurulumu QR kod gerektirdiği için web sitesi üzerinden yapılmalıdır.

## Lisans

Bu proje [MIT](LICENSE) lisansı altındadır.

## Yasal Uyarı

Bu uygulama bağımsız bir açık kaynak projesidir; delidehset.com ile resmi bir bağı bulunmamaktadır.
