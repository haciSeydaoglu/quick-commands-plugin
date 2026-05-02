package com.quickcommands.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.quickcommands.util.TerminalExecutor

/**
 * Tekil terminal komutu çalıştırma action'ı
 * %70+ Claude ile yazıldı
 */
class RunCommandAction(
    private val displayName: String,
    private val command: String,
    private val commandId: String,
    private val askTitleOnRun: Boolean = false
) : AnAction(displayName), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sekmeBasligi = sekmeBasligiBelirle(project) ?: return
        TerminalExecutor.runInNewTab(project, command, sekmeBasligi)
    }

    /**
     * Sekme başlığını döner. askTitleOnRun açıksa kullanıcıya sorar:
     *  - boş girilirse sadece komut adı kullanılır
     *  - iptal edilirse null döner ve komut çalıştırılmaz
     */
    private fun sekmeBasligiBelirle(project: com.intellij.openapi.project.Project): String? {
        if (!askTitleOnRun) return displayName
        val girdi = Messages.showInputDialog(
            project,
            "Sekme başlığı için etiket girin (boş bırakılırsa sadece '$displayName' kullanılır):",
            "Sekme Başlığı",
            Messages.getQuestionIcon()
        ) ?: return null
        return if (girdi.isBlank()) displayName else "$displayName — $girdi"
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
