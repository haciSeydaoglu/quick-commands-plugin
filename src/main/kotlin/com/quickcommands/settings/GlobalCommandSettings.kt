package com.quickcommands.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Global commands - Visible in all projects (Application level)
 * 70%+ written with Claude
 */
@State(
    name = "TerminalCommanderGlobalSettings",
    storages = [Storage(value = "terminalCommanderGlobal.xml", roamingType = RoamingType.DEFAULT)],
    category = SettingsCategory.TOOLS
)
@Service(Service.Level.APP)
class GlobalCommandSettings : PersistentStateComponent<GlobalCommandSettings> {

    var commands: MutableList<CommandEntry> = createDefaultCommands()
    var claudeSkillsEnabled: Boolean = true
    var claudeSkillsDangerousMode: Boolean = true
    var pluginSkillsEnabled: Boolean = true
    var codexSkillsEnabled: Boolean = true
    var codexSkillsDangerousMode: Boolean = true
    var showEmojis: Boolean = true

    companion object {
        fun getInstance(): GlobalCommandSettings {
            return ApplicationManager.getApplication().getService(GlobalCommandSettings::class.java)
        }

        fun createDefaultCommands(): MutableList<CommandEntry> = mutableListOf(
            // Marka emojileri her halukarda basta: claude \u2192 \uD83D\uDD38, codex \u2192 \u25AA\uFE0F
            // Yolo modu ust grup, sade ad \u2014 paralel sekmeleri ayirt etmek icin askTitleOnRun = true
            CommandEntry("\uD83D\uDD38claude", "claude --dangerously-skip-permissions /plan", askTitleOnRun = true),
            CommandEntry("\u25AA\uFE0Fcodex", "codex --yolo", askTitleOnRun = true),
            CommandEntry.createSeparator(),
            CommandEntry("\uD83D\uDD38../claude", "cd ../ && claude --dangerously-skip-permissions /plan", askTitleOnRun = true),
            CommandEntry("\u25AA\uFE0F../codex", "cd ../ && codex --yolo", askTitleOnRun = true),
            CommandEntry.createSeparator(),
            // Yolo olmayan \u2014 sonda \uD83D\uDD12 kilit ile yolo olmadigi belirtilir
            CommandEntry("\uD83D\uDD38claude\uD83D\uDD12", "claude /plan", askTitleOnRun = true),
            CommandEntry("\u25AA\uFE0Fcodex\uD83D\uDD12", "codex", askTitleOnRun = true),
            CommandEntry.createSeparator(),
            CommandEntry("\uD83D\uDD38../claude\uD83D\uDD12", "cd ../ && claude /plan", askTitleOnRun = true),
            CommandEntry("\u25AA\uFE0F../codex\uD83D\uDD12", "cd ../ && codex", askTitleOnRun = true)
        )
    }

    override fun getState(): GlobalCommandSettings = this

    override fun loadState(state: GlobalCommandSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
