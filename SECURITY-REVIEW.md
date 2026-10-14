# Mobil güvenlik incelemesi — 26 Eylül 2026

Kapsam: Android/iOS ortak uygulama kodu, oturum ve ağ istemcisi, platform güvenlik ayarları, Android release çalışma zamanı ve iOS arm64 derleme bağımlılıkları. Kullanıcının mevcut yerel değişiklikleri korunmuştur.

## Düzeltilen bulgular

| Bulgu | Düzeltme |
| --- | --- |
| Giriş, token ve muhasebe verileri HTTP üzerinden gönderiliyordu. | Üç servis de tek HTTPS API adresini kullanıyor: `https://www.hesapbenim.com/api`. Android açık metin trafiği kapatıldı, iOS ATS istisnası kaldırıldı. HTTPS `/api/config` kimliksiz isteği TLS doğrulaması açıkken 200 döndü. |
| Genel istemci her hedefe token ekleyebiliyordu. | HTTPS, alan adı, port ve API yolunu doğrulayan gönderim kontrolü eklendi. Kullanıcı bilgisi içeren URL'ler ve otomatik yönlendirmeler engellendi. Bağlantı/istek zaman aşımı eklendi. |
| Oturum token'ı SharedPreferences/NSUserDefaults içinde düz metindi. | Android'de Android Keystore anahtarıyla AES-GCM şifreleme; iOS'ta yalnız cihazda, kilit açıkken erişilen Keychain kaydı kullanılıyor. Şifreleme başarısız olursa düz metne dönüş yok. Eski düz metin token siliniyor ve yeniden giriş gerekiyor. |
| Android oturum ve muhasebe verileri yedeklemeye açıktı. | Otomatik yedekleme kapatıldı; bulut yedeği ve cihaz aktarımı dışlamaları eklendi. FileProvider tüm önbellek yerine yalnız `raporlar/` diziniyle sınırlandı. |
| API ve ekran loglarında müşteri/ürün, sunucu yanıtı ve hata ayrıntıları bulunuyordu. | Hassas veri logları kaldırıldı veya sabit metne çevrildi. API hata mesajlarına ham sunucu gövdesi eklenmiyor. |
| Çıkış yalnız cihazdaki token'ı siliyordu. | Yerel çıkıştan sonra kapatılan token ile `/api/oturum/cikis` çağrılıyor. Ayrı istemci yeni bir girişin token'ını yanlışlıkla iptal etmiyor. |
| Ktor 2.3.12 bilinen CVE eşleşmesi taşıyordu. | Tüm Ktor modülleri ve test motoru 2.3.13'e yükseltildi. |

Ktor bulgusu [CVE-2024-49580 / GHSA-8qv4-773j-c979](https://github.com/advisories/GHSA-8qv4-773j-c979): HttpCache bilgi sızıntısı; uygulamada HttpCache kurulmadığından mevcut akışta bu saldırı yolu saptanmadı. [Üreticinin 2.3.13 değişiklik kaydı](https://github.com/ktorio/ktor/releases/tag/2.3.13) düzeltmeyi içerir.

Gradle ve GitHub Actions güncellemelerini takip etmek için Dependabot yapılandırması eklendi; depoya gönderildiğinde GitHub üzerinde kullanılabilir.

## Doğrulama

- OSV `querybatch`: Android release ağacında 191 benzersiz Maven paketi. Önce bir eşleşme (Ktor), sonra sıfır eşleşme.
- iOS arm64 derleme ağacında 140 benzersiz Maven paketi: sıfır OSV eşleşmesi. Platforma özel Maven adları için danışma kayıtlarının kapsamı sınırlı olabilir.
- `:shared:testAndroidHostTest`: 11 test, sıfır hata. Ağ testleri HTTP, başka alan adı, benzer alan adı, farklı port, API dışı yol ve kullanıcı bilgisi içeren URL'leri engelliyor; yönlendirme izlenmediğini ve çıkıştan sonra token gönderilmediğini kontrol ediyor.
- `:androidApp:assembleDebug`: başarılı.
- `:shared:compileCommonMainKotlinMetadata`: başarılı.
- `:androidApp:lintDebug`: başarılı, sıfır hata. 20 uyarı yeni sürüm bildirimleri ve eski/kullanılmayan kaynaklarla ilgili; bunlar doğrulanmış güvenlik açığı olarak değerlendirilmedi.
- `git diff --check`: başarılı.

Yerel tarama girdileri/çıktıları ve derleme logları `build/security-audit/` altında. APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Sınırlar ve yayın notu

- Windows üzerinde iOS uygulaması derlenmedi veya cihazda çalıştırılmadı. Ortak metadata derlemesi iOS Keychain uygulamasının Xcode derlemesi yerine geçmez. Yayın öncesi macOS/Xcode üzerinde giriş, uygulamayı yeniden açma, çıkış ve eski oturum geçişi doğrulanmalı.
- Android Keystore davranışı fiziksel cihazda/enstrümantasyon testiyle doğrulanmadı; Android kaynak derlemesi ve ortak ağ testleri başarılı.
- Canlı hesapla giriş, kayıt, veri değiştirme veya çıkış isteği yapılmadı. Çıkışta ağ yoksa yerel oturum kapanır, sunucu token'ının iptali garanti edilemez; sunucu süresi dolana kadar geçerli kalabilir.
- Güvenlik taraması tam penetrasyon testi değildir; OSV'de eşleşme olmaması bilinmeyen açıklara karşı garanti değildir. Sunucu/web tarafı bu değişiklik kapsamında yayımlanmadı.
- Eski HTTP trafiğinde daha önce açığa çıkmış token'lar cihazdan silinerek uzaktan geçersiz hale getirilemez. Yeni girişin sunucudaki token yenileme davranışı önemlidir.
- Kullanıcı güncellemeden sonra bir kez yeniden giriş yapar. Düzeltmeler kaynak kodunda ve yerel debug APK'dadır; mağazaya dağıtım yapılmadı.
