package com.quickcommands.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Global commands - Visible in all projects (Application level)
 * 70%+ written with Claude
 */
@State(
    name = "TerminalCommanderGlobalSettings",
    storages = [Storage(value = "terminalCommanderGlobal.xml", roamingType = RoamingType.DEFAULT)]
)
@Service(Service.Level.APP)
class GlobalCommandSettings : PersistentStateComponent<GlobalCommandSettings> {

    var commands: MutableList<CommandEntry> = createDefaultCommands()

    companion object {
        fun getInstance(): GlobalCommandSettings {
            return ApplicationManager.getApplication().getService(GlobalCommandSettings::class.java)
        }

        fun createDefaultCommands(): MutableList<CommandEntry> = mutableListOf(
            CommandEntry("claude", "claude"),
            CommandEntry("claude\uD83D\uDE80", "claude --dangerously-skip-permissions"),
            CommandEntry.createSeparator(),
            CommandEntry("codex", "codex"),
            CommandEntry("codex\uD83D\uDE80", "codex --yolo"),
            CommandEntry.createSeparator(),
            CommandEntry("../claude", "cd ../ && claude"),
            CommandEntry("../claude\uD83D\uDE80", "cd ../ && claude --dangerously-skip-permissions"),
            CommandEntry.createSeparator(),
            CommandEntry("../codex", "cd ../ && codex"),
            CommandEntry("../codex\uD83D\uDE80", "cd ../ && codex --yolo")
        )
    }

    override fun getState(): GlobalCommandSettings = this

    override fun loadState(state: GlobalCommandSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
