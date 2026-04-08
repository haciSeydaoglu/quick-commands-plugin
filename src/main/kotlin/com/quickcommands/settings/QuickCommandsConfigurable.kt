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
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Color
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
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
 * Two tabs: Global Commands and Project Commands
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
        val separator: Boolean = false
    )

    private var globalTableModel: DefaultTableModel? = null
    private var projectTableModel: DefaultTableModel? = null
    private var mainPanel: JComponent? = null

    override fun getDisplayName(): String = "Quick Commands"

    override fun createComponent(): JComponent {
        val tabbedPane = JBTabbedPane()

        // Tab 1: Global Commands
        globalTableModel = createTableModel()
        loadCommands(globalTableModel!!, GlobalCommandSettings.getInstance().commands)
        tabbedPane.addTab(
            "Global Commands",
            createCommandPanel(globalTableModel!!, "Visible in all projects", isGlobal = true)
        )

        // Tab 2: Project Commands
        projectTableModel = createTableModel()
        loadCommands(projectTableModel!!, ProjectCommandSettings.getInstance(project).commands)
        tabbedPane.addTab(
            "Project Commands",
            createCommandPanel(projectTableModel!!, "Visible only in '${project.name}' project", isGlobal = false)
        )

        mainPanel = tabbedPane
        return tabbedPane
    }

    private fun createTableModel(): DefaultTableModel {
        return object : DefaultTableModel(arrayOf("Name", "Command", "IsSeparator"), 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                val isSep = getValueAt(row, 2) as? Boolean ?: false
                return !isSep && column < 2
            }
        }
    }

    private fun loadCommands(model: DefaultTableModel, commands: List<CommandEntry>) {
        model.rowCount = 0
        commands.forEach { cmd ->
            if (cmd.separator) {
                model.addRow(arrayOf(SEPARATOR_DISPLAY, "", true))
            } else {
                model.addRow(arrayOf(cmd.name, cmd.command, false))
            }
        }
    }

    private fun createCommandPanel(
        tableModel: DefaultTableModel,
        hint: String,
        isGlobal: Boolean
    ): JComponent {
        val table = JBTable(tableModel)

        // IsSeparator sütununu görünümden gizle (data model'de kalıyor)
        table.removeColumn(table.columnModel.getColumn(2))

        table.columnModel.getColumn(0).preferredWidth = 150
        table.columnModel.getColumn(1).preferredWidth = 400

        // Separator satırları gri ve ortalı göster
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

        // Ctrl+Shift+S kısayolu ile separator ekleme
        val separatorShortcut = KeyStroke.getKeyStroke("control shift S")

        val toolbar = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val uniqueName = generateUniqueName(tableModel, "New Command")
                tableModel.addRow(arrayOf(uniqueName, "", false))
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
                    if (isGlobal) {
                        loadCommands(tableModel, GlobalCommandSettings.createDefaultCommands())
                    } else {
                        tableModel.rowCount = 0
                    }
                }
            })

        // Global sekmede import/export butonlari
        if (isGlobal) {
            toolbar.addExtraAction(object : AnAction(
                "Export",
                "Export commands as JSON",
                AllIcons.ToolbarDecorator.Export
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    showExportPopup(tableModel, e)
                }
            })
            toolbar.addExtraAction(object : AnAction(
                "Import",
                "Import commands from JSON",
                AllIcons.ToolbarDecorator.Import
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    showImportPopup(tableModel, table, e)
                }
            })
        }

        val toolbarPanel = toolbar.createPanel()

        // Separator kısayolunu tabloya kaydet
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
        tableModel.insertRow(insertAt, arrayOf(SEPARATOR_DISPLAY, "", true))
        table.setRowSelectionInterval(insertAt, insertAt)
    }

    override fun isModified(): Boolean {
        val globalModified = !commandsMatch(
            globalTableModel!!,
            GlobalCommandSettings.getInstance().commands
        )
        val projectModified = !commandsMatch(
            projectTableModel!!,
            ProjectCommandSettings.getInstance(project).commands
        )
        return globalModified || projectModified
    }

    private fun commandsMatch(model: DefaultTableModel, commands: List<CommandEntry>): Boolean {
        if (model.rowCount != commands.size) return false
        for (i in 0 until model.rowCount) {
            val isSep = model.getValueAt(i, 2) as? Boolean ?: false
            if (commands[i].separator != isSep) return false
            if (!isSep) {
                val name = model.getValueAt(i, 0) as? String ?: ""
                val command = model.getValueAt(i, 1) as? String ?: ""
                if (commands[i].name != name || commands[i].command != command) return false
            }
        }
        return true
    }

    override fun apply() {
        saveTableToSettings(globalTableModel!!, GlobalCommandSettings.getInstance().commands)
        saveTableToSettings(projectTableModel!!, ProjectCommandSettings.getInstance(project).commands)
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
                if (name.isNotBlank() || command.isNotBlank()) {
                    commands.add(CommandEntry(name, command))
                }
            }
        }
    }

    override fun reset() {
        loadCommands(globalTableModel!!, GlobalCommandSettings.getInstance().commands)
        loadCommands(projectTableModel!!, ProjectCommandSettings.getInstance(project).commands)
    }

    override fun disposeUIResources() {
        mainPanel = null
        globalTableModel = null
        projectTableModel = null
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
        // Onceki degerleri saklamak icin
        var oncekiDegerler = mutableMapOf<Int, String>()

        // Baslangic degerlerini kaydet
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
                    // Satir ekleme/silme durumunda onceki degerleri guncelle
                    oncekiDegerler.clear()
                    for (i in 0 until model.rowCount) {
                        val isSep = model.getValueAt(i, 2) as? Boolean ?: false
                        if (!isSep) {
                            oncekiDegerler[i] = (model.getValueAt(i, 0) as? String) ?: ""
                        }
                    }
                    return
                }

                // Sadece isim sutunu (0) degistiginde kontrol et
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

    private fun showExportPopup(model: DefaultTableModel, e: AnActionEvent) {
        val popup = JPopupMenu()

        val panoyaKopyala = JMenuItem("Copy to Clipboard")
        panoyaKopyala.addActionListener { exportToClipboard(model) }
        popup.add(panoyaKopyala)

        val dosyayaKaydet = JMenuItem("Save to File")
        dosyayaKaydet.addActionListener { exportToFile(model) }
        popup.add(dosyayaKaydet)

        val component = e.inputEvent?.component
        if (component != null) {
            popup.show(component, 0, component.height)
        } else {
            // Fallback: mainPanel uzerinde goster
            mainPanel?.let { popup.show(it, it.width / 2, it.height / 2) }
        }
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
                if (name.isNotBlank() || command.isNotBlank()) {
                    komutlar.add(ExportCommand(name = name, command = command))
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

    private fun showImportPopup(model: DefaultTableModel, table: JBTable, e: AnActionEvent) {
        val popup = JPopupMenu()

        val panodanYapistir = JMenuItem("Paste from Clipboard")
        panodanYapistir.addActionListener { importFromClipboard(model, table) }
        popup.add(panodanYapistir)

        val dosyadanYukle = JMenuItem("Load from File")
        dosyadanYukle.addActionListener { importFromFile(model, table) }
        popup.add(dosyadanYukle)

        val component = e.inputEvent?.component
        if (component != null) {
            popup.show(component, 0, component.height)
        } else {
            mainPanel?.let { popup.show(it, it.width / 2, it.height / 2) }
        }
    }

    private fun parseCommandsFromJson(json: String): List<ExportCommand>? {
        return try {
            val data = gson.fromJson(json, ExportData::class.java)
            if (data?.commands == null) null else data.commands
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    private fun importFromClipboard(model: DefaultTableModel, table: JBTable) {
        val icerik = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        if (icerik.isNullOrBlank()) {
            Messages.showWarningDialog("No valid content found in clipboard.", "Import Error")
            return
        }
        processImport(icerik, model, table)
    }

    private fun importFromFile(model: DefaultTableModel, table: JBTable) {
        val dosyaSecici = JFileChooser()
        dosyaSecici.dialogTitle = "Load Commands"
        dosyaSecici.fileFilter = FileNameExtensionFilter("JSON File (*.json)", "json")

        if (dosyaSecici.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            val icerik = dosyaSecici.selectedFile.readText(Charsets.UTF_8)
            processImport(icerik, model, table)
        }
    }

    private fun processImport(json: String, model: DefaultTableModel, table: JBTable) {
        val komutlar = parseCommandsFromJson(json)
        if (komutlar == null || komutlar.isEmpty()) {
            Messages.showWarningDialog(
                "Invalid or empty JSON format. Please use a valid Quick Commands export file.",
                "Import Error"
            )
            return
        }

        // Onizleme dialog'u goster
        if (!showImportPreviewDialog(komutlar)) return

        // Upsert uygula
        applyUpsert(komutlar, model)
        table.clearSelection()
    }

    /** Import onizleme dialog'u - kullaniciya iceri aktarilacak komutlari gosterir */
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
                model.addRow(arrayOf(SEPARATOR_DISPLAY, "", true))
            } else {
                val isim = cmd.name?.trim() ?: ""
                val komut = cmd.command ?: ""
                if (isim.isBlank() && komut.isBlank()) return@forEach

                // Ayni isimli satir var mi kontrol et
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
                    // Guncelle
                    model.setValueAt(komut, mevcutSatir, 1)
                } else {
                    // Yeni ekle
                    model.addRow(arrayOf(isim, komut, false))
                }
            }
        }
    }
}
