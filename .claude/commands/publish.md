---
description: Versiyonu artir, CHANGELOG guncelle, build al ve JetBrains Marketplace'e yukle
allowed-tools: Bash(./gradlew:*), Read, Edit, Glob, Grep
---

# Publish Plugin

Plugin'i JetBrains Marketplace'e yayinla.

## Adimlar

1. **Mevcut versiyonu oku**: `gradle.properties` dosyasindaki `pluginVersion` degerini oku.

2. **Versiyon bump**: PATCH versiyonu 1 artir (ornegin 1.4.1 → 1.4.2). Sadece PATCH artir, MINOR/MAJOR artirma.

3. **CHANGELOG guncelle**: `CHANGELOG.md` dosyasinin basina yeni versiyon bolumunu ekle. Son commit'lerden (`git log` ile onceki versiyondan bu yana yapilan commit'lere bak) anlamli bir degisiklik listesi olustur. CHANGELOG her zaman Ingilizce yazilir.

4. **Clean build**: `./gradlew clean buildPlugin` komutunu calistir. Hata varsa dur ve kullaniciya bildir.

5. **Publish**: `./gradlew publishPlugin` komutunu calistir. `PUBLISH_TOKEN` environment variable'i zaten tanimli olmali.

6. **Sonuc**: Basarili ise yeni versiyon numarasini ve marketplace'e yuklendigini bildir.

## Onemli

- Build veya publish basarisiz olursa DURMA, hatayi kullaniciya bildir.
- `PUBLISH_TOKEN` tanimli degilse kullaniciyi uyar.
- Versiyon artirimi sadece PATCH seviyesinde yapilir. MINOR veya MAJOR artirmak icin kullanicidan onay iste.
