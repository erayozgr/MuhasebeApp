This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
## Mobil tasarım (17 Eylül 2026)

Müşteriler, tedarikçiler, ürünler, satışlar, alışlar, masraflar, stoklar ve raporlar ortak `ui/BrandDesign.kt` bileşenlerini kullanır. Logo ile uyumlu lacivert/turkuaz başlıklar, semantik renkli özetler, uzun ad ve tutarları ayrı satırlara yerleştiren kayıt kartları ve açıklamalı ekleme düğmeleri eklendi. Ortak kod dışında platforma özel UI bağımlılığı eklenmedi.

Sistem çubukları, iOS güvenli alanı ve klavye boşlukları `MainStructure` içinde yönetilir; iç Scaffold'lar bu boşlukları tekrar eklemez. Yatay filtre kaydırmasını engelleyebilen tüm-ekran geri sürükleme kaldırıldı; görünür geri düğmeleri ve Android sistem geri davranışı korunur. Satış/alış ve stok formları kaydırılabilir; etkin ana ekran kimliği konfigürasyon değişiminde korunur.

Doğrulama: `:androidApp:assembleDebug` ve `:shared:compileCommonMainKotlinMetadata` başarılı. Android emülatöründe sekiz ekran yerel örnek verilerle açıldı; uygulama çökme kaydı görülmedi. Küçük ekran ve 1.3 yazı ölçeğinde ürün ekranı/formu incelendi. Görsel testin geçici giriş noktası ve örnek API adresi kaldırıldı; son APK gerçek uygulama girişi ve mevcut API ayarlarıyla yeniden derlendi. Gerçek hesaplarla uçtan uca işlemler veya iOS cihaz/simülatör testi yapılmadı; iOS doğrulaması macOS/Xcode gerektirir.

## Mobil abonelik kontrolü

Android ve iOS ortak kodunda hesap bilgileri açılışta, uygulama ön plana geldiğinde ve işlem modüllerine geçişte sunucudan kontrol edilir. Son ödeme tarihi geçmişse ana menüde web sitesi üzerinden ödeme uyarısı ve siteyi açan düğme görünür. Ürün, müşteri, tedarikçi, satış, alış, masraf, stok ve rapor modülleri kapanır; hesap bilgileri ve çıkış kullanılabilir. Açık oturumda vade dolunca işlem ekranı ana menüye döner. Kontrol yüklenirken veya başarısız olduğunda işlem modülleri açılmaz; tekrar deneme sunulur.

Hesap bilgilerinde son ödeme tarihi gösterilir. Sunucunun saat dilimsiz ISO tarihleri Türkiye saatine göre okunur; eski epoch milisaniye değerleri de desteklenir. Tarihi bulunmayan eski hesaplar web ile aynı şekilde açık kalır. Yeni kayıtların 7 günlük denemesi sunucudaki kayıt doğrulama akışından gelir; mobil uygulama süreyi yeniden başlatmaz. Erişim kontrolü mobil arayüz içindir; doğrudan API abonelik yetkilendirmesi değiştirilmemiştir.

Testler: `:shared:testAndroidHostTest` tarih biçimleri, Türkiye saat dilimi, tam vade anı, eksik/geçersiz tarih senaryolarını kapsar. Android paketi `:androidApp:assembleDebug`, ortak kod `:shared:compileCommonMainKotlinMetadata` ile doğrulanır. iOS cihaz/simülatör doğrulaması macOS/Xcode gerektirir.

## Kayıt sırasında sözleşme kabulü

Kayıt formunda varsayılan olarak boş olan Kullanıcı Sözleşmesi kutusu zorunludur; sözleşme ve KVKK sayfaları web tarayıcısında açılır. Kayıt isteği `kullaniciSozlesmesiKabul` ve `sozlesmeSurumu` alanlarını gönderir. Sunucu kabul tarihini/sürümünü saklar; onaysız/eski istemci kayıtlarını reddeder. Sürüm `AppConfig.SOZLESME_SURUMU` ile web ve sunucudaki sürümle birlikte güncellenmelidir. Sözleşme onayı pazarlama izni değildir.

## Web ile mobil işlem uyumu (21 Eylül 2026)

- Android/iOS ortak veri modellerinde stok, alış/satış miktarları ve stok hareketleri `Double` kullanır. Virgül ve nokta kabul edilir; giriş iki ondalık basamakla sınırlandırılır. Geçersiz ve karışık ayırıcılı metinler kaydedilmez. Ürün birimi miktar etiketinde gösterilir; negatif stok düzenlemede korunur.
- Alışlar/Satışlar listesindeki kalem simgesi mevcut kaydın tarihini, miktarını ve fiyatını düzenler; kalem kaldırma/ürün ekleme ve kaydetme öncesi onay vardır. Cari hesap korunur. Kaydetme başarısızsa form açık kalır. Kalemleri yüklenmemiş kayıtta düzenleme kapalıdır.
- Müşteri/tedarikçi satırındaki ödeme simgesi seçili carinin ödeme formunu açar. Geçmiş işlemler, toplamlar, avans ve işlem sonrası bakiye gösterilir. Geçmişteki Düzenle düğmesi mevcut tahsilat/ödemenin tutarını ve tarihini değiştirir. Kayıt sırasında düğmeler kilitlenir; API hatası formda gösterilir. Başarılı kayıtta cari liste yenilenir.
- Satışta yetersiz stok engel değildir; ürün seçimi, sepet ve düzenleme onayında negatif stok uyarısı gösterilir. Masraf kategorisi serbest yazılır; en sık ve son kullanılan beş kategori önerilir.
- SQLDelight `1.sqm` geçişi dört miktar alanını REAL tipine çevirirken kayıtları ve kimlikleri korur. Excel miktar hücreleri küsüratı korur ve para biçimi kullanmaz.

Tahsilat/ödeme düzenleme için sunucudaki yeni `PUT /api/tahsilatlar/{id}` ve `PUT /api/tedarikci-odemeleri/{id}` uçları gereklidir. Güncel sunucu WAR paketi yayımlanmadan eski sunucu bu iki işlemi desteklemez. Mevcut kayıt silinip yeniden oluşturulmaz; cari bakiye tutar farkıyla güncellenir.

Doğrulama komutları: `:androidApp:assembleDebug`, `:shared:testAndroidHostTest`, `:shared:compileCommonMainKotlinMetadata`; yerel SQLite geçiş testi `python shared/src/commonTest/migration_test.py`. iOS cihaz/simülatör testi macOS/Xcode gerektirir; canlı hesaplarla işlem yapılmadı.
