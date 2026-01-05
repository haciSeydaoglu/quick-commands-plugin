package com.quickcommands.settings

import java.util.UUID

/**
 * Terminal komutu veri modeli
 * %70+ Claude ile yazıldı
 */
data class CommandEntry(
    var name: String = "",
    var command: String = "",
    var id: String = UUID.randomUUID().toString()
) {
    // State serialization için no-arg constructor gerekli
    constructor() : this("", "", UUID.randomUUID().toString())

    fun copy(): CommandEntry = CommandEntry(name, command, id)
}
