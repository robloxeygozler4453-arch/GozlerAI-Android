# GözlerAI Android

GözlerAI modellerini Android cihazlarda yerel olarak çalıştırmak için Kotlin + Jetpack Compose uygulaması.

## Özellikler

- Yalnızca resmî GözlerAI v1 ve v2 GGUF modelleri için katalog
- Hugging Face üzerinden uygulamanın özel depolama alanına model indirme
- İndirme ilerlemesi, model seçme, yükleme ve silme
- llama.cpp C/C++ motoru ile cihaz üzerinde metin üretimi
- Model yüklendikten sonra çevrimdışı sohbet
- Sohbet geçmişini cihazda saklama
- Android ARM64 ve x86_64 hedefleri

## Modeller

| Sürüm | Hugging Face deposu | Dosya |
|---|---|---|
| GözlerAI v2 | https://huggingface.co/TurkeyProje/GozlerAI-1B-v2-GGUF | GozlerAI-v2-Q4_K_M.gguf |
| GözlerAI v1 | https://huggingface.co/TurkeyProje/GozlerAI-1B-Merged-GGUF | GozlerAI-1B-Q4_K_M.gguf |

Her model yaklaşık 808 MB'dır. İndirmeden önce cihazda en az 1.5 GB boş alan bırakılması önerilir. Modelin bellekte çalışması için ek RAM gerekir; gerçek ihtiyaç cihaza göre değişir.

## Teknik yapı

- Android minSdk 26 (Android 8.0)
- Kotlin, Jetpack Compose, Material 3
- Android NDK 27.2.12479018 ve CMake 3.22.1
- llama.cpp, sabitlenmiş upstream commit: `956591f4570aa6636895d90439892fef7c075b17`
- JNI köprüsü; GGUF yükleme ve yerel token üretimi

## Derleme

Android Studio ile depoyu açın. JDK 17, Android SDK 35, NDK 27.2.12479018 ve CMake 3.22.1 kurulu olmalıdır.

Ardından:
```bash
gradle assembleDebug
```

İlk derlemede CMake, sabitlenmiş llama.cpp kaynağını GitHub'dan indirir; bu nedenle internet bağlantısı gerekir. APK oluşturulduktan sonra model dosyası ayrıca uygulama içinden indirilir.

GitHub Actions her `main` güncellemesinde ARM64 ve x86_64 debug APK derlemeyi dener ve başarılı APK'yı artifact olarak yayımlar.

## Durum ve sınırlamalar

Bu depo, uygulama kodunu ve otomatik derleme hattını içerir. Derleme hattının yeşil sonuç verdiği ve APK'nın gerçek cihazda test edildiği doğrulanmadan sürüm "cihazda test edildi" olarak kabul edilmemelidir.

- Model çıkarımı CPU üzerinden yapılır; GPU hızlandırması henüz etkinleştirilmedi.
- 1B Q4 modelin performansı cihazın RAM'ine, işlemcisine ve termal durumuna bağlıdır.
- Uygulama Android 8.0+ ve ARM64/x86_64 hedefler; tüm cihazlarda aynı hız veya bellek uyumluluğu garanti edilmez.
- İlk sürümde sohbet geçmişi ve modeller uygulamanın özel depolama alanında tutulur.

## Lisans

Uygulama kodu ve kullanılan model/llama.cpp lisansları ilgili kaynak depolarının lisans koşullarına tabidir.
