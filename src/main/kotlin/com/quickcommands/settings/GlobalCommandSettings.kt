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

    var commands: MutableList<CommandEntry> = mutableListOf(
        CommandEntry("Claude", "claude"),
        CommandEntry("Claude (Super Permission)", "claude --dangerously-skip-permissions")
    )

    companion object {
        fun getInstance(): GlobalCommandSettings {
            return ApplicationManager.getApplication().getService(GlobalCommandSettings::class.java)
        }
    }

    override fun getState(): GlobalCommandSettings = this

    override fun loadState(state: GlobalCommandSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
