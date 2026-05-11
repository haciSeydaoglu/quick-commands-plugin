package com.quickcommands.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.icons.AllIcons
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JMenuItem
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * Quick Commands settings page
 * Three tabs: Global Commands, Project Commands, Settings
 * 70%+ written with Claude
 */
class QuickCommandsConfigurable(private val project: Project) : Configurable {

    companion object {
        private const val SEPARATOR_DISPLAY = "─────────────"
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }

    /** JSON import/export veri modeli */
    private data class ExportData(
        val version: Int = 1,
        val commands: List<ExportCommand> = emptyList()
    )

    private data class ExportCommand(
        val name: String? = null,
        val command: String? = null,
        val separator: Boolean = false,
        val askTitleOnRun: Boolean = false
    )

    private var globalTableModel: DefaultTableModel? = null
    private var projectTableModel: DefaultTableModel? = null

    // Settings tab checkbox'lari
    private var autoDetectCheckbox: JBCheckBox? = null
    private var claudeSkillsCheckbox: JBCheckBox? = null
    private var claudeDangerousCheckbox: JBCheckBox? = null
    private var pluginSkillsCheckbox: JBCheckBox? = null
    private var agentSkillsCheckbox: JBCheckBox? = null
    private var agentDangerousCheckbox: JBCheckBox? = null
    private var showEmojisCheckbox: JBCheckBox? = null

    private var mainPanel: JComponent? = null

    override fun getDisplayName(): String = "Quick Commands"

    override fun createComponent(): JComponent {
        val tabbedPane = JBTabbedPane()

        // Tab 1: Global Commands
        globalTableModel = createTableModel()
        loadCommands(globalTableModel!!, GlobalCommandSettings.getInstance().commands)
        tabbedPane.addTab(
            "Global Commands",
            createCommandPanel(globalTableModel!!, "Visible in all projects")
        )

        // Tab 2: Project Commands
        projectTableModel = createTableModel()
        loadCommands(projectTableModel!!, ProjectCommandSettings.getInstance(project).commands)
        tabbedPane.addTab(
            "Project Commands",
            createCommandPanel(projectTableModel!!, "Visible only in '${project.name}' project")
        )

        // Tab 3: Settings
        tabbedPane.addTab("Settings", settingsTabOlustur())

        mainPanel = tabbedPane
        return tabbedPane
    }

    // ── Settings Tab ────────────────────────────────────────────────────

    private fun settingsTabOlustur(): JComponent {
        val globalSettings = GlobalCommandSettings.getInstance()
        val projectSettings = ProjectCommandSettings.getInstance(project)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(12)

        // ── Project Settings bolumu ──
        val projectSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Project Settings")
            alignmentX = Component.LEFT_ALIGNMENT
        }

        autoDetectCheckbox = JBCheckBox("Include auto-detected scripts (package.json, composer.json)").apply {
            isSelected = projectSettings.autoDetectScripts
            alignmentX = Component.LEFT_ALIGNMENT
        }
        projectSection.add(autoDetectCheckbox!!)

        panel.add(projectSection)
        panel.add(javax.swing.Box.createVerticalStrut(12))

        // ── Global Settings bolumu ──
        val globalSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Global Settings")
            alignmentX = Component.LEFT_ALIGNMENT
        }

        claudeSkillsCheckbox = JBCheckBox("Include Claude Code skills and commands").apply {
            isSelected = globalSettings.claudeSkillsEnabled
            alignmentX = Component.LEFT_ALIGNMENT
        }
        globalSection.add(claudeSkillsCheckbox!!)

        claudeDangerousCheckbox = JBCheckBox("Run with --dangerously-skip-permissions").apply {
            isSelected = globalSettings.claudeSkillsDangerousMode
            isEnabled = globalSettings.claudeSkillsEnabled
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyLeft(20)
        }
        globalSection.add(claudeDangerousCheckbox!!)

        pluginSkillsCheckbox = JBCheckBox("Include plugin skills").apply {
            isSelected = globalSettings.pluginSkillsEnabled
            isEnabled = globalSettings.claudeSkillsEnabled
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyLeft(20)
        }
        globalSection.add(pluginSkillsCheckbox!!)

        agentSkillsCheckbox = JBCheckBox("Include Agent skills").apply {
            isSelected = globalSettings.agentSkillsEnabled
            alignmentX = Component.LEFT_ALIGNMENT
        }
        globalSection.add(agentSkillsCheckbox!!)

        agentDangerousCheckbox = JBCheckBox("Run Agent skills with codex --yolo (skip approvals)").apply {
            isSelected = globalSettings.agentSkillsDangerousMode
            isEnabled = globalSettings.agentSkillsEnabled
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyLeft(20)
        }
        globalSection.add(agentDangerousCheckbox!!)

        showEmojisCheckbox = JBCheckBox("Show emoji icons").apply {
            isSelected = globalSettings.showEmojis
            alignmentX = Component.LEFT_ALIGNMENT
        }
        globalSection.add(showEmojisCheckbox!!)

        // Claude checkbox degistiginde alt checkbox'lari aktif/pasif yap
        claudeSkillsCheckbox!!.addChangeListener {
            val aktif = claudeSkillsCheckbox!!.isSelected
            claudeDangerousCheckbox!!.isEnabled = aktif
            pluginSkillsCheckbox!!.isEnabled = aktif
        }

        // Agent skills checkbox degistiginde alt checkbox'i aktif/pasif yap
        agentSkillsCheckbox!!.addChangeListener {
            agentDangerousCheckbox!!.isEnabled = agentSkillsCheckbox!!.isSelected
        }

        panel.add(globalSection)
        panel.add(javax.swing.Box.createVerticalStrut(12))

        // ── Import / Export bolumu ──
        val importExportSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Import / Export (Global Commands)")
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

        val exportButton = JButton("Export").apply {
            addActionListener { showExportPopupFromButton(this) }
        }
        buttonPanel.add(exportButton)

        val importButton = JButton("Import").apply {
            addActionListener { showImportPopupFromButton(this) }
        }
        buttonPanel.add(importButton)

        importExportSection.add(buttonPanel)

        panel.add(importExportSection)

        // Alt bosluk doldurucu
        panel.add(javax.swing.Box.createVerticalGlue())

        return panel
    }

    // ── Command Table ───────────────────────────────────────────────────

    private fun createTableModel(): DefaultTableModel {
        return object : DefaultTableModel(arrayOf("Name", "Command", "IsSeparator", "Ask Title"), 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                val isSep = getValueAt(row, 2) as? Boolean ?: false
                if (isSep) return false
                // 0 = Name, 1 = Command, 3 = Ask Title (checkbox); 2 = IsSeparator gizli
                return column == 0 || column == 1 || column == 3
            }

            override fun getColumnClass(column: Int): Class<*> {
                return if (column == 3) java.lang.Boolean::class.java else String::class.java
            }
        }
    }

    private fun loadCommands(model: DefaultTableModel, commands: List<CommandEntry>) {
        model.rowCount = 0
        commands.forEach { cmd ->
            if (cmd.separator) {
                model.addRow(arrayOf(SEPARATOR_DISPLAY, "", true, false))
            } else {
                model.addRow(arrayOf(cmd.name, cmd.command, false, cmd.askTitleOnRun))
            }
        }
    }

    private fun createCommandPanel(
        tableModel: DefaultTableModel,
        hint: String
    ): JComponent {
        val table = JBTable(tableModel)

        // IsSeparator sutununu gorunumden gizle (data model'de kaliyor)
        // removeColumn sonrası: view 0=Name, view 1=Command, view 2=Ask Title (model 3)
        table.removeColumn(table.columnModel.getColumn(2))

        table.columnModel.getColumn(0).preferredWidth = 150
        table.columnModel.getColumn(1).preferredWidth = 400
        table.columnModel.getColumn(2).preferredWidth = 80
        table.columnModel.getColumn(2).maxWidth = 100

        // Separator satirlari gri ve ortali goster
        val separatorRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean,
                hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                val isSep = tableModel.getValueAt(row, 2) as? Boolean ?: false
                if (isSep) {
                    text = if (column == 0) SEPARATOR_DISPLAY else ""
                    foreground = Color.GRAY
                    horizontalAlignment = CENTER
                } else {
                    foreground = if (isSelected) table.selectionForeground else table.foreground
                    horizontalAlignment = LEFT
                }
                return comp
            }
        }
        table.columnModel.getColumn(0).cellRenderer = separatorRenderer
        table.columnModel.getColumn(1).cellRenderer = separatorRenderer

        // Ask Title sütunu: separator satırlarında boş, diğerlerinde checkbox
        val askTitleRenderer = object : DefaultTableCellRenderer() {
            private val checkboxKomponent = JBCheckBox().apply {
                horizontalAlignment = javax.swing.SwingConstants.CENTER
                isBorderPainted = false
            }

            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean,
                hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val isSep = tableModel.getValueAt(row, 2) as? Boolean ?: false
                if (isSep) {
                    val comp = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column)
                    background = if (isSelected) table.selectionBackground else table.background
                    return comp
                }
                checkboxKomponent.isSelected = (value as? Boolean) ?: false
                checkboxKomponent.background = if (isSelected) table.selectionBackground else table.background
                return checkboxKomponent
            }
        }
        table.columnModel.getColumn(2).cellRenderer = askTitleRenderer

        // Ctrl+Shift+S kisayolu ile separator ekleme
        val separatorShortcut = KeyStroke.getKeyStroke("control shift S")

        val toolbar = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val uniqueName = generateUniqueName(tableModel, "New Command")
                tableModel.addRow(arrayOf(uniqueName, "", false, false))
                val row = tableModel.rowCount - 1
                table.editCellAt(row, 0)
                table.setRowSelectionInterval(row, row)
            }
            .setRemoveAction {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    tableModel.removeRow(selectedRow)
                }
            }
            .setMoveUpAction {
                val selectedRow = table.selectedRow
                if (selectedRow > 0) {
                    tableModel.moveRow(selectedRow, selectedRow, selectedRow - 1)
                    table.setRowSelectionInterval(selectedRow - 1, selectedRow - 1)
                }
            }
            .setMoveDownAction {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0 && selectedRow < tableModel.rowCount - 1) {
                    tableModel.moveRow(selectedRow, selectedRow, selectedRow + 1)
                    table.setRowSelectionInterval(selectedRow + 1, selectedRow + 1)
                }
            }
            .addExtraAction(object : AnAction(
                "Add Separator (${KeymapUtil.getKeystrokeText(separatorShortcut)})",
                "Add a separator line",
                AllIcons.General.SeparatorH
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    addSeparatorRow(tableModel, table)
                }
            })
            .addExtraAction(object : AnAction(
                "Reset to Defaults",
                "Reset commands to default list",
                AllIcons.Actions.Rollback
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    if (tableModel === globalTableModel) {
                        loadCommands(tableModel, GlobalCommandSettings.createDefaultCommands())
                    } else {
                        tableModel.rowCount = 0
                    }
                }
            })

        val toolbarPanel = toolbar.createPanel()

        // Separator kisayolunu tabloya kaydet
        table.registerKeyboardAction(
            { addSeparatorRow(tableModel, table) },
            separatorShortcut,
            JComponent.WHEN_FOCUSED
        )

        // Duplicate isim kontrolu icin listener
        addDuplicateNameListener(tableModel)

        val panel = JPanel(BorderLayout())
        panel.add(JBLabel("<html><i>$hint</i></html>"), BorderLayout.NORTH)
        panel.add(toolbarPanel, BorderLayout.CENTER)

        return panel
    }

    private fun addSeparatorRow(tableModel: DefaultTableModel, table: JBTable) {
        val selectedRow = table.selectedRow
        val insertAt = if (selectedRow >= 0) selectedRow + 1 else tableModel.rowCount
        tableModel.insertRow(insertAt, arrayOf(SEPARATOR_DISPLAY, "", true, false))
        table.setRowSelectionInterval(insertAt, insertAt)
    }

    // ── Persistence ─────────────────────────────────────────────────────

    override fun isModified(): Boolean {
        val globalModified = !commandsMatch(
            globalTableModel!!,
            GlobalCommandSettings.getInstance().commands
        )
        val projectModified = !commandsMatch(
            projectTableModel!!,
            ProjectCommandSettings.getInstance(project).commands
        )

        val projectSettings = ProjectCommandSettings.getInstance(project)
        val globalSettings = GlobalCommandSettings.getInstance()

        val settingsModified =
            autoDetectCheckbox?.isSelected != projectSettings.autoDetectScripts
                    || claudeSkillsCheckbox?.isSelected != globalSettings.claudeSkillsEnabled
                    || claudeDangerousCheckbox?.isSelected != globalSettings.claudeSkillsDangerousMode
                    || pluginSkillsCheckbox?.isSelected != globalSettings.pluginSkillsEnabled
                    || agentSkillsCheckbox?.isSelected != globalSettings.agentSkillsEnabled
                    || agentDangerousCheckbox?.isSelected != globalSettings.agentSkillsDangerousMode
                    || showEmojisCheckbox?.isSelected != globalSettings.showEmojis

        return globalModified || projectModified || settingsModified
    }

    private fun commandsMatch(model: DefaultTableModel, commands: List<CommandEntry>): Boolean {
        if (model.rowCount != commands.size) return false
        for (i in 0 until model.rowCount) {
            val isSep = model.getValueAt(i, 2) as? Boolean ?: false
            if (commands[i].separator != isSep) return false
            if (!isSep) {
                val name = model.getValueAt(i, 0) as? String ?: ""
                val command = model.getValueAt(i, 1) as? String ?: ""
                val askTitle = model.getValueAt(i, 3) as? Boolean ?: false
                if (commands[i].name != name || commands[i].command != command
                    || commands[i].askTitleOnRun != askTitle) return false
            }
        }
        return true
    }

    override fun apply() {
        val globalSettings = GlobalCommandSettings.getInstance()
        saveTableToSettings(globalTableModel!!, globalSettings.commands)

        val projectSettings = ProjectCommandSettings.getInstance(project)
        saveTableToSettings(projectTableModel!!, projectSettings.commands)

        // Settings tab
        projectSettings.autoDetectScripts = autoDetectCheckbox?.isSelected ?: true
        globalSettings.claudeSkillsEnabled = claudeSkillsCheckbox?.isSelected ?: true
        globalSettings.claudeSkillsDangerousMode = claudeDangerousCheckbox?.isSelected ?: true
        globalSettings.pluginSkillsEnabled = pluginSkillsCheckbox?.isSelected ?: true
        globalSettings.agentSkillsEnabled = agentSkillsCheckbox?.isSelected ?: true
        globalSettings.agentSkillsDangerousMode = agentDangerousCheckbox?.isSelected ?: true
        globalSettings.showEmojis = showEmojisCheckbox?.isSelected ?: true
    }

    private fun saveTableToSettings(model: DefaultTableModel, commands: MutableList<CommandEntry>) {
        commands.clear()
        for (i in 0 until model.rowCount) {
            val isSep = model.getValueAt(i, 2) as? Boolean ?: false
            if (isSep) {
                commands.add(CommandEntry.createSeparator())
            } else {
                val name = model.getValueAt(i, 0) as? String ?: ""
                val command = model.getValueAt(i, 1) as? String ?: ""
                val askTitle = model.getValueAt(i, 3) as? Boolean ?: false
                if (name.isNotBlank() || command.isNotBlank()) {
                    commands.add(CommandEntry(name = name, command = command, askTitleOnRun = askTitle))
                }
            }
        }
    }

    override fun reset() {
        loadCommands(globalTableModel!!, GlobalCommandSettings.getInstance().commands)
        loadCommands(projectTableModel!!, ProjectCommandSettings.getInstance(project).commands)

        val projectSettings = ProjectCommandSettings.getInstance(project)
        val globalSettings = GlobalCommandSettings.getInstance()

        autoDetectCheckbox?.isSelected = projectSettings.autoDetectScripts
        claudeSkillsCheckbox?.isSelected = globalSettings.claudeSkillsEnabled
        claudeDangerousCheckbox?.isSelected = globalSettings.claudeSkillsDangerousMode
        claudeDangerousCheckbox?.isEnabled = globalSettings.claudeSkillsEnabled
        pluginSkillsCheckbox?.isSelected = globalSettings.pluginSkillsEnabled
        pluginSkillsCheckbox?.isEnabled = globalSettings.claudeSkillsEnabled
        agentSkillsCheckbox?.isSelected = globalSettings.agentSkillsEnabled
        agentDangerousCheckbox?.isSelected = globalSettings.agentSkillsDangerousMode
        agentDangerousCheckbox?.isEnabled = globalSettings.agentSkillsEnabled
        showEmojisCheckbox?.isSelected = globalSettings.showEmojis
    }

    override fun disposeUIResources() {
        mainPanel = null
        globalTableModel = null
        projectTableModel = null
        autoDetectCheckbox = null
        claudeSkillsCheckbox = null
        claudeDangerousCheckbox = null
        pluginSkillsCheckbox = null
        agentSkillsCheckbox = null
        agentDangerousCheckbox = null
        showEmojisCheckbox = null
    }

    // ── Duplicate isim kontrolu ──────────────────────────────────────────

    /** Tablodaki mevcut isimlere bakarak benzersiz isim uretir */
    private fun generateUniqueName(model: DefaultTableModel, baseName: String): String {
        val mevcutIsimler = collectNames(model)
        if (baseName !in mevcutIsimler) return baseName
        var sayac = 2
        while ("$baseName ($sayac)" in mevcutIsimler) sayac++
        return "$baseName ($sayac)"
    }

    /** Tablodaki tum komut isimlerini toplar (separator haric) */
    private fun collectNames(model: DefaultTableModel, excludeRow: Int = -1): Set<String> {
        val isimler = mutableSetOf<String>()
        for (i in 0 until model.rowCount) {
            if (i == excludeRow) continue
            val isSep = model.getValueAt(i, 2) as? Boolean ?: false
            if (!isSep) {
                val name = (model.getValueAt(i, 0) as? String)?.trim() ?: ""
                if (name.isNotBlank()) isimler.add(name)
            }
        }
        return isimler
    }

    /** Isim duzenlendiginde duplicate kontrol eden listener */
    private fun addDuplicateNameListener(model: DefaultTableModel) {
        var oncekiDegerler = mutableMapOf<Int, String>()

        for (i in 0 until model.rowCount) {
            val isSep = model.getValueAt(i, 2) as? Boolean ?: false
            if (!isSep) {
                oncekiDegerler[i] = (model.getValueAt(i, 0) as? String) ?: ""
            }
        }

        model.addTableModelListener(object : TableModelListener {
            private var dinlemeyiAtla = false

            override fun tableChanged(e: TableModelEvent) {
                if (dinlemeyiAtla) return
                if (e.type != TableModelEvent.UPDATE) {
                    oncekiDegerler.clear()
                    for (i in 0 until model.rowCount) {
                        val isSep = model.getValueAt(i, 2) as? Boolean ?: false
                        if (!isSep) {
                            oncekiDegerler[i] = (model.getValueAt(i, 0) as? String) ?: ""
                        }
                    }
                    return
                }

                if (e.column != 0) return
                val satir = e.firstRow
                if (satir < 0 || satir >= model.rowCount) return

                val isSep = model.getValueAt(satir, 2) as? Boolean ?: false
                if (isSep) return

                val yeniIsim = (model.getValueAt(satir, 0) as? String)?.trim() ?: ""
                if (yeniIsim.isBlank()) return

                val digerIsimler = collectNames(model, excludeRow = satir)
                if (yeniIsim in digerIsimler) {
                    dinlemeyiAtla = true
                    val eskiDeger = oncekiDegerler[satir] ?: ""
                    model.setValueAt(eskiDeger, satir, 0)
                    dinlemeyiAtla = false
                    Messages.showWarningDialog(
                        "A command named '$yeniIsim' already exists.",
                        "Duplicate Name"
                    )
                } else {
                    oncekiDegerler[satir] = yeniIsim
                }
            }
        })
    }

    // ── Export islemleri ──────────────────────────────────────────────────

    private fun showExportPopupFromButton(component: JComponent) {
        val popup = JPopupMenu()

        val panoyaKopyala = JMenuItem("Copy to Clipboard")
        panoyaKopyala.addActionListener { exportToClipboard(globalTableModel!!) }
        popup.add(panoyaKopyala)

        val dosyayaKaydet = JMenuItem("Save to File")
        dosyayaKaydet.addActionListener { exportToFile(globalTableModel!!) }
        popup.add(dosyayaKaydet)

        popup.show(component, 0, component.height)
    }

    private fun exportCommandsToJson(model: DefaultTableModel): String {
        val komutlar = mutableListOf<ExportCommand>()
        for (i in 0 until model.rowCount) {
            val isSep = model.getValueAt(i, 2) as? Boolean ?: false
            if (isSep) {
                komutlar.add(ExportCommand(separator = true))
            } else {
                val name = model.getValueAt(i, 0) as? String ?: ""
                val command = model.getValueAt(i, 1) as? String ?: ""
                val askTitle = model.getValueAt(i, 3) as? Boolean ?: false
                if (name.isNotBlank() || command.isNotBlank()) {
                    komutlar.add(ExportCommand(name = name, command = command, askTitleOnRun = askTitle))
                }
            }
        }
        return gson.toJson(ExportData(commands = komutlar))
    }

    private fun exportToClipboard(model: DefaultTableModel) {
        val json = exportCommandsToJson(model)
        CopyPasteManager.getInstance().setContents(StringSelection(json))
        Messages.showInfoMessage("Commands copied to clipboard.", "Export Successful")
    }

    private fun exportToFile(model: DefaultTableModel) {
        val json = exportCommandsToJson(model)
        val dosyaSecici = JFileChooser()
        dosyaSecici.dialogTitle = "Save Commands"
        dosyaSecici.fileFilter = FileNameExtensionFilter("JSON File (*.json)", "json")
        dosyaSecici.selectedFile = File("quick-commands.json")

        if (dosyaSecici.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            var dosya = dosyaSecici.selectedFile
            if (!dosya.name.endsWith(".json")) {
                dosya = File(dosya.absolutePath + ".json")
            }
            dosya.writeText(json, Charsets.UTF_8)
            Messages.showInfoMessage("Commands saved:\n${dosya.absolutePath}", "Export Successful")
        }
    }

    // ── Import islemleri ──────────────────────────────────────────────────

    private fun showImportPopupFromButton(component: JComponent) {
        val popup = JPopupMenu()

        val panodanYapistir = JMenuItem("Paste from Clipboard")
        panodanYapistir.addActionListener { importFromClipboard(globalTableModel!!) }
        popup.add(panodanYapistir)

        val dosyadanYukle = JMenuItem("Load from File")
        dosyadanYukle.addActionListener { importFromFile(globalTableModel!!) }
        popup.add(dosyadanYukle)

        popup.show(component, 0, component.height)
    }

    private fun parseCommandsFromJson(json: String): List<ExportCommand>? {
        return try {
            val data = gson.fromJson(json, ExportData::class.java)
            if (data?.commands == null) null else data.commands
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    private fun importFromClipboard(model: DefaultTableModel) {
        val icerik = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        if (icerik.isNullOrBlank()) {
            Messages.showWarningDialog("No valid content found in clipboard.", "Import Error")
            return
        }
        processImport(icerik, model)
    }

    private fun importFromFile(model: DefaultTableModel) {
        val dosyaSecici = JFileChooser()
        dosyaSecici.dialogTitle = "Load Commands"
        dosyaSecici.fileFilter = FileNameExtensionFilter("JSON File (*.json)", "json")

        if (dosyaSecici.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            val icerik = dosyaSecici.selectedFile.readText(Charsets.UTF_8)
            processImport(icerik, model)
        }
    }

    private fun processImport(json: String, model: DefaultTableModel) {
        val komutlar = parseCommandsFromJson(json)
        if (komutlar == null || komutlar.isEmpty()) {
            Messages.showWarningDialog(
                "Invalid or empty JSON format. Please use a valid Quick Commands export file.",
                "Import Error"
            )
            return
        }

        if (!showImportPreviewDialog(komutlar)) return

        applyUpsert(komutlar, model)
    }

    /** Import onizleme dialog'u */
    private fun showImportPreviewDialog(komutlar: List<ExportCommand>): Boolean {
        val dialog = object : DialogWrapper(project, false) {
            init {
                title = "Import Preview"
                setOKButtonText("Import")
                setCancelButtonText("Cancel")
                init()
            }

            override fun createCenterPanel(): JComponent {
                val onizlemeModel = DefaultTableModel(arrayOf("Name", "Command"), 0)
                komutlar.forEach { cmd ->
                    if (cmd.separator) {
                        onizlemeModel.addRow(arrayOf(SEPARATOR_DISPLAY, ""))
                    } else {
                        onizlemeModel.addRow(arrayOf(cmd.name ?: "", cmd.command ?: ""))
                    }
                }

                val tablo = JBTable(onizlemeModel)
                tablo.isEnabled = false
                tablo.columnModel.getColumn(0).preferredWidth = 150
                tablo.columnModel.getColumn(1).preferredWidth = 400

                val panel = JPanel(BorderLayout())
                panel.add(
                    JBLabel("<html><i>The following commands will be imported. Existing commands with the same name will be updated.</i></html>"),
                    BorderLayout.NORTH
                )
                panel.add(JBScrollPane(tablo), BorderLayout.CENTER)
                panel.preferredSize = Dimension(600, 300)

                return panel
            }
        }
        return dialog.showAndGet()
    }

    /** Upsert: ayni isimde varsa guncelle, yoksa ekle */
    private fun applyUpsert(komutlar: List<ExportCommand>, model: DefaultTableModel) {
        komutlar.forEach { cmd ->
            if (cmd.separator) {
                model.addRow(arrayOf(SEPARATOR_DISPLAY, "", true, false))
            } else {
                val isim = cmd.name?.trim() ?: ""
                val komut = cmd.command ?: ""
                if (isim.isBlank() && komut.isBlank()) return@forEach

                var mevcutSatir = -1
                for (i in 0 until model.rowCount) {
                    val isSep = model.getValueAt(i, 2) as? Boolean ?: false
                    if (!isSep) {
                        val mevcutIsim = (model.getValueAt(i, 0) as? String)?.trim() ?: ""
                        if (mevcutIsim == isim) {
                            mevcutSatir = i
                            break
                        }
                    }
                }

                if (mevcutSatir >= 0) {
                    model.setValueAt(komut, mevcutSatir, 1)
                    model.setValueAt(cmd.askTitleOnRun, mevcutSatir, 3)
                } else {
                    model.addRow(arrayOf(isim, komut, false, cmd.askTitleOnRun))
                }
            }
        }
    }
}
