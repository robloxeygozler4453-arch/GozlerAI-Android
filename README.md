# GözlerAI Android

GözlerAI modelleri için Android uygulaması.

## İlk prototip
- Kotlin + Jetpack Compose arayüzü
- Sohbet, model kütüphanesi ve ayarlar sekmeleri
- Resmî GözlerAI v1/v2 model kartları
- GitHub Actions ile debug APK derleme iş akışı

## Henüz tamamlanmadı
- GGUF indirme ve model yönetimi
- llama.cpp JNI/NDK entegrasyonu
- Gerçek çevrimdışı model çıkarımı

## Derleme
Android Studio ile açın; JDK 17 ve Android SDK 35 gerekir. Ardından `gradle assembleDebug` çalıştırın.

Hedef: Android 8.0+ (API 26), ARM64 ve x86_64. Model çalıştırmak için yeterli RAM ve depolama gerekir.

Modeller:
- https://huggingface.co/TurkeyProje/GozlerAI-1B-v2-GGUF
- https://huggingface.co/TurkeyProje/GozlerAI-1B-Merged-GGUF
