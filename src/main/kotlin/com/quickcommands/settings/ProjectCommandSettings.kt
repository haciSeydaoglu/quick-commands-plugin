package com.quickcommands.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Proje komutları - Sadece ilgili projede görünür (Project level)
 * %70+ Claude ile yazıldı
 */
@State(
    name = "TerminalCommanderProjectSettings",
    storages = [Storage("terminalCommander.xml")]
)
@Service(Service.Level.PROJECT)
class ProjectCommandSettings : PersistentStateComponent<ProjectCommandSettings> {

    var commands: MutableList<CommandEntry> = mutableListOf()

    companion object {
        fun getInstance(project: Project): ProjectCommandSettings {
            return project.getService(ProjectCommandSettings::class.java)
        }
    }

    override fun getState(): ProjectCommandSettings = this

    override fun loadState(state: ProjectCommandSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
