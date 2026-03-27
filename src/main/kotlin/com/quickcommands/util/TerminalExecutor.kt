package com.quickcommands.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

/**
 * Terminal'de yeni tab açıp komut çalıştırır
 */
object TerminalExecutor {

    fun runInNewTab(project: Project, command: String, tabName: String) {
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID) ?: return

        val isTerminalAlreadyVisible = toolWindow.isVisible

        if (isTerminalAlreadyVisible) {
            // Terminal zaten açıksa direkt yeni tab oluştur
            createAndExecute(project, command, tabName)
        } else {
            // Terminal kapalıysa önce widget'ı oluştur, sonra pencereyi göster
            // activate() yerine show() kullanarak varsayılan boş sekme oluşmasını önle
            try {
                createAndExecute(project, command, tabName)
                toolWindow.show()
            } catch (e: Exception) {
                // Terminal açılamazsa sessizce devam et
            }
        }
    }

    private fun createAndExecute(project: Project, command: String, tabName: String) {
        val workingDir = project.basePath ?: System.getProperty("user.home")
        val widget = TerminalToolWindowManager.getInstance(project)
            .createShellWidget(workingDir, tabName, true, false)
        widget.sendCommandToExecute(command)
    }
}
