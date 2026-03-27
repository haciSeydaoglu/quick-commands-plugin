package com.quickcommands.settings

import com.intellij.util.xmlb.Converter

/**
 * IntelliJ XML serializer'ın supplementary plane Unicode karakterleri
 * (emojiler vb.) attribute value'larda düşürmesini önleyen converter.
 * Kaydetme sırasında emojileri \\U+XXXXX formatına encode eder,
 * okuma sırasında geri decode eder.
 */
class EmojiSafeConverter : Converter<String>() {

    override fun toString(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            val codePoint = value.codePointAt(i)
            if (codePoint > 0xFFFF) {
                sb.append("\\U+${String.format("%05X", codePoint)}")
            } else {
                sb.append(value[i])
            }
            i += Character.charCount(codePoint)
        }
        return sb.toString()
    }

    override fun fromString(value: String): String {
        return value.replace(Regex("\\\\U\\+([0-9A-Fa-f]+)")) { match ->
            val codePoint = match.groupValues[1].toInt(16)
            String(Character.toChars(codePoint))
        }
    }
}
