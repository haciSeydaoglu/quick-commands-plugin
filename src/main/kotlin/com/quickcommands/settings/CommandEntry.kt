package com.quickcommands.settings

import com.intellij.util.xmlb.annotations.OptionTag
import java.util.UUID

/**
 * Terminal komutu veri modeli
 * %70+ Claude ile yazıldı
 */
data class CommandEntry(
    @OptionTag(converter = EmojiSafeConverter::class)
    var name: String = "",
    @OptionTag(converter = EmojiSafeConverter::class)
    var command: String = "",
    var id: String = UUID.randomUUID().toString(),
    var separator: Boolean = false,
    // Çalışırken sekme başlığı için kullanıcıdan etiket sorulsun mu
    var askTitleOnRun: Boolean = false
) {
    // State serialization için no-arg constructor gerekli
    constructor() : this("", "", UUID.randomUUID().toString(), false, false)

    fun copy(): CommandEntry = CommandEntry(name, command, id, separator, askTitleOnRun)

    companion object {
        fun createSeparator(): CommandEntry = CommandEntry(separator = true)
    }
}
