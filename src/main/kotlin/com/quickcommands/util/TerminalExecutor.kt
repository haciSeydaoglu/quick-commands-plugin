package com.quickcommands.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

/**
 * Terminal'de yeni tab açıp komut çalıştırır
 * %70+ Claude ile yazıldı
 */
object TerminalExecutor {

    fun runInNewTab(project: Project, command: String, tabName: String) {
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)

        toolWindow?.let { tw ->
            // Basit activate - focus yönetimini tamamen ToolWindow'a bırak
            // JetBrains önerisi: "requestFocus doğrudan kullanılmamalı, ToolWindowManager kendi yönetir"
            tw.activate({
                try {
                    val widget = terminalManager.createLocalShellWidget(
                        project.basePath ?: System.getProperty("user.home"),
                        tabName,
                        true  // activateWindow = true
                    )

                    if (widget is ShellTerminalWidget) {
                        widget.executeCommand(command)
                    }

                    // Manuel focus müdahalesi YOK - ToolWindow'un kendi mekanizması çalışsın

                } catch (e: Exception) {
                    // Fallback: Basit terminal tab aç
                    terminalManager.createLocalShellWidget(
                        project.basePath ?: System.getProperty("user.home"),
                        tabName,
                        true
                    )
                }
            }, true)
        }
    }
}
