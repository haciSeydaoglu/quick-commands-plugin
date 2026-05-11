---
name: publish
description: Quick Commands IntelliJ plugin'ini JetBrains Marketplace'e yayınlamak gerektiğinde kullan. Patch sürüm artırır, CHANGELOG günceller, build alır ve publish işlemini çalıştırır.
---

# Publish Plugin

Plugin'i JetBrains Marketplace'e yayınla.

## Adımlar

1. **Mevcut versiyonu oku**: `gradle.properties` dosyasındaki `pluginVersion` değerini oku.

2. **Versiyon bump**: PATCH versiyonu 1 artır (örneğin 1.4.1 -> 1.4.2). Sadece PATCH artır, MINOR/MAJOR artırma.

3. **CHANGELOG güncelle**: `CHANGELOG.md` dosyasının başına yeni versiyon bölümünü ekle. Son commit'lerden (`git log` ile önceki versiyondan bu yana yapılan commit'lere bak) anlamlı bir değişiklik listesi oluştur. CHANGELOG her zaman İngilizce yazılır.

4. **Clean build**: `./gradlew clean buildPlugin` komutunu çalıştır. Hata varsa dur ve kullanıcıya bildir.

5. **Publish**: `./gradlew publishPlugin` komutunu çalıştır. `PUBLISH_TOKEN` environment variable'ı zaten tanımlı olmalı.

6. **Sonuç**: Başarılı ise yeni versiyon numarasını ve marketplace'e yüklendiğini bildir.

## Önemli

- Build veya publish başarısız olursa durma, hatayı kullanıcıya bildir.
- `PUBLISH_TOKEN` tanımlı değilse kullanıcıyı uyar.
- Versiyon artırımı sadece PATCH seviyesinde yapılır. MINOR veya MAJOR artırmak için kullanıcıdan onay iste.
