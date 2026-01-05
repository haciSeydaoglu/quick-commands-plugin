package com.quickcommands.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.quickcommands.util.TerminalExecutor

/**
 * Tekil terminal komutu çalıştırma action'ı
 * %70+ Claude ile yazıldı
 */
class RunCommandAction(
    private val displayName: String,
    private val command: String,
    private val commandId: String
) : AnAction(displayName), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        TerminalExecutor.runInNewTab(project, command, displayName)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
