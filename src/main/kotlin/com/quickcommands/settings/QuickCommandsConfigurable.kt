package com.quickcommands.settings

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke
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
    }

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
                tableModel.addRow(arrayOf("New Command", "", false))
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
            .createPanel()

        // Separator kısayolunu tabloya kaydet
        table.registerKeyboardAction(
            { addSeparatorRow(tableModel, table) },
            separatorShortcut,
            JComponent.WHEN_FOCUSED
        )

        val panel = JPanel(BorderLayout())
        panel.add(JBLabel("<html><i>$hint</i></html>"), BorderLayout.NORTH)
        panel.add(toolbar, BorderLayout.CENTER)

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
}
