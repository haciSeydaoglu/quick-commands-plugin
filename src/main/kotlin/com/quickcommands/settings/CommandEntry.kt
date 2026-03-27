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
    var separator: Boolean = false
) {
    // State serialization için no-arg constructor gerekli
    constructor() : this("", "", UUID.randomUUID().toString(), false)

    fun copy(): CommandEntry = CommandEntry(name, command, id, separator)

    companion object {
        fun createSeparator(): CommandEntry = CommandEntry(separator = true)
    }
}
