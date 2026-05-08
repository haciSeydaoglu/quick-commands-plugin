package com.quickcommands.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAware
import com.quickcommands.services.ClaudeSkillCategory
import com.quickcommands.services.ClaudeSkillDetectionService
import com.quickcommands.services.ClaudeSkillGroup
import com.quickcommands.services.CodexSkillDetectionService
import com.quickcommands.services.CodexSkillGroup
import com.quickcommands.services.DetectedScriptGroup
import com.quickcommands.services.ScriptDetectionService
import com.quickcommands.services.ScriptDetectionService.Companion.emojiEslestir
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
                if (cmd.separator) {
                    actions.add(Separator.create())
                } else {
                    actions.add(RunCommandAction(cmd.name, cmd.command, cmd.id, cmd.askTitleOnRun))
                }
            }
        }

        // Project Commands
        val projectCommands = ProjectCommandSettings.getInstance(project).commands
        if (projectCommands.isNotEmpty()) {
            actions.add(Separator.create("Project: ${project.name}"))
            projectCommands.forEach { cmd ->
                if (cmd.separator) {
                    actions.add(Separator.create())
                } else {
                    actions.add(RunCommandAction(cmd.name, cmd.command, cmd.id, cmd.askTitleOnRun))
                }
            }
        }

        // Otomatik tespit edilen scriptler (package.json, composer.json)
        val projectSettings = ProjectCommandSettings.getInstance(project)
        val globalSettings = GlobalCommandSettings.getInstance()
        if (projectSettings.autoDetectScripts) {
            val service = ScriptDetectionService.getInstance(project)
            val gruplar = service.getDetectedScripts()
            if (gruplar.isNotEmpty()) {
                actions.add(Separator.create("Scripts"))
                scriptMenuOlustur(gruplar, actions, globalSettings.showEmojis)
            }
        }

        // Claude Skills & Commands
        if (globalSettings.claudeSkillsEnabled) {
            val claudeService = ClaudeSkillDetectionService.getInstance(project)
            val skillGruplar = claudeService.getDetectedSkills()
                .filter { globalSettings.pluginSkillsEnabled || it.category != ClaudeSkillCategory.PLUGIN }
            if (skillGruplar.isNotEmpty()) {
                claudeMenuOlustur(skillGruplar, actions, globalSettings)
            }
        }

        // Codex Skills
        if (globalSettings.codexSkillsEnabled) {
            val codexService = CodexSkillDetectionService.getInstance(project)
            val codexGruplar = codexService.getDetectedSkills()
            if (codexGruplar.isNotEmpty()) {
                codexMenuOlustur(codexGruplar, actions, globalSettings)
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

    /**
     * Tespit edilen script gruplarini klasor bazli separator basliklariyla menuye ekler.
     * Her package.json/composer.json bir submenu olarak gosterilir.
     * Root gruplar dogrudan "Scripts" altinda, alt klasordekiler ust klasor adina gore gruplanir.
     */
    private fun scriptMenuOlustur(gruplar: List<DetectedScriptGroup>, actions: MutableList<AnAction>, emojiGoster: Boolean) {
        // Root gruplari duz liste olarak dogrudan ekle
        gruplar.filter { it.relativePath == "root" }.forEach { grup ->
            grup.scripts.forEach { script ->
                actions.add(RunCommandAction(
                    scriptEtiketOlustur(script.name, emojiGoster),
                    script.command,
                    "detected-${grup.type.name}-root-${script.name}"
                ))
            }
        }

        // Alt klasordeki gruplari ilk klasor adina gore grupla, submenu olarak ekle
        val altKlasorGruplari = gruplar
            .filter { it.relativePath != "root" }
            .groupBy { it.relativePath.substringBefore("/") }

        for ((ustKlasor, altGruplar) in altKlasorGruplari) {
            actions.add(Separator.create(ustKlasor))
            altGruplar.forEach { grup ->
                val kisaYol = if (grup.relativePath.contains("/")) {
                    grup.relativePath.substringAfter("/")
                } else {
                    grup.relativePath
                }
                actions.add(scriptGrupSubmenu(grup, kisaYol, emojiGoster))
            }
        }
    }

    /** Script adi icin emoji prefix ekler (ayara bagli) */
    private fun scriptEtiketOlustur(ad: String, emojiGoster: Boolean): String {
        return if (emojiGoster) "${emojiEslestir(ad)} $ad" else ad
    }

    /** Tek bir script grubu icin submenu olusturur */
    private fun scriptGrupSubmenu(grup: DetectedScriptGroup, etiket: String, emojiGoster: Boolean): DefaultActionGroup {
        val altMenu = DefaultActionGroup("${grup.type.displayPrefix}: $etiket", true)
        grup.scripts.forEach { script ->
            altMenu.add(RunCommandAction(
                scriptEtiketOlustur(script.name, emojiGoster),
                script.command,
                "detected-${grup.type.name}-${grup.relativePath}-${script.name}"
            ))
        }
        return altMenu
    }

    /** Claude skill gruplarini kategoriye gore menuye ekler */
    private fun claudeMenuOlustur(
        gruplar: List<ClaudeSkillGroup>,
        actions: MutableList<AnAction>,
        globalSettings: GlobalCommandSettings
    ) {
        val onEk = if (globalSettings.claudeSkillsDangerousMode) {
            "claude --dangerously-skip-permissions"
        } else {
            "claude"
        }

        for (grup in gruplar) {
            actions.add(Separator.create("Claude: ${grup.category.displayName}"))
            grup.skills.forEach { skill ->
                val komut = "$onEk ${skill.slashCommand}"
                val etiket = if (globalSettings.showEmojis) {
                    val emoji = emojiEslestir(skill.name)
                    "$emoji ${skill.slashCommand}"
                } else {
                    skill.slashCommand
                }
                actions.add(RunCommandAction(
                    etiket,
                    komut,
                    "claude-${skill.name}"
                ))
            }
        }
    }

    /** Codex skill gruplarini kategoriye gore menuye ekler */
    private fun codexMenuOlustur(
        gruplar: List<CodexSkillGroup>,
        actions: MutableList<AnAction>,
        globalSettings: GlobalCommandSettings
    ) {
        val onEk = if (globalSettings.codexSkillsDangerousMode) {
            "codex --yolo"
        } else {
            "codex"
        }

        for (grup in gruplar) {
            actions.add(Separator.create("Codex: ${grup.category.displayName}"))
            grup.skills.forEach { skill ->
                // Tek tirnak: shell $skill-ad'ini env var olarak expand etmesin
                val komut = "$onEk '${skill.invokeToken}'"
                val etiket = if (globalSettings.showEmojis) {
                    val emoji = emojiEslestir(skill.name)
                    "$emoji ${skill.invokeToken}"
                } else {
                    skill.invokeToken
                }
                actions.add(RunCommandAction(
                    etiket,
                    komut,
                    "codex-${skill.name}"
                ))
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
