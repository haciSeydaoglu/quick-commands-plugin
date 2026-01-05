package com.quickcommands.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAware
import com.quickcommands.settings.GlobalCommandSettings
import com.quickcommands.settings.ProjectCommandSettings

/**
 * Dropdown menu in Terminal toolbar
 * Lists global and project-specific commands
 * 70%+ written with Claude
 */
class QuickCommandsDropdownAction : DefaultActionGroup(), DumbAware {

    init {
        isPopup = true
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return emptyArray()
        val actions = mutableListOf<AnAction>()

        // Global Commands
        val globalCommands = GlobalCommandSettings.getInstance().commands
        if (globalCommands.isNotEmpty()) {
            actions.add(Separator.create("Global"))
            globalCommands.forEach { cmd ->
                actions.add(RunCommandAction(cmd.name, cmd.command, cmd.id))
            }
        }

        // Project Commands
        val projectCommands = ProjectCommandSettings.getInstance(project).commands
        if (projectCommands.isNotEmpty()) {
            actions.add(Separator.create("Project: ${project.name}"))
            projectCommands.forEach { cmd ->
                actions.add(RunCommandAction(cmd.name, cmd.command, cmd.id))
            }
        }

        // Settings link
        actions.add(Separator.create())
        actions.add(OpenSettingsAction())

        return actions.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isPopupGroup = true
        e.presentation.text = "Quick Commands"
        e.presentation.description = "Run predefined terminal commands"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
