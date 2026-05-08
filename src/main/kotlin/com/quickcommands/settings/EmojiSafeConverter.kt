package com.quickcommands.settings

import com.intellij.util.xmlb.Converter

/**
 * IntelliJ XML serializer'ın supplementary plane Unicode karakterleri
 * (emojiler vb.) attribute value'larda düşürmesini önleyen converter.
 * Kaydetme sırasında emojileri \\U+XXXXXX formatına (sabit 6 hex) encode eder,
 * okuma sırasında geri decode eder.
 */
class EmojiSafeConverter : Converter<String>() {

    override fun toString(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            val codePoint = value.codePointAt(i)
            if (codePoint > 0xFFFF) {
                sb.append("\\U+${String.format("%06X", codePoint)}")
            } else {
                sb.append(value[i])
            }
            i += Character.charCount(codePoint)
        }
        return sb.toString()
    }

    override fun fromString(value: String): String {
        // {1,6} bounded greedy + longest-valid-prefix: eski 5-hex formatlı kayıtlarda
        // emoji'nin ardından gelen hex harf (a-f/A-F) match'e sızdığında 0x10FFFF üstü
        // değer üretilip Character.toChars patlıyordu. Geçerli code point bulunana kadar
        // hex'ten sondan kırparak geri çekiliyoruz; kalan hex karakterler literal kalır.
        return value.replace(Regex("\\\\U\\+([0-9A-Fa-f]{1,6})")) { match ->
            val hex = match.groupValues[1]
            for (len in hex.length downTo 1) {
                val codePoint = hex.substring(0, len).toInt(16)
                if (codePoint in 0x10000..0x10FFFF) {
                    return@replace String(Character.toChars(codePoint)) + hex.substring(len)
                }
            }
            match.value
        }
    }
}
