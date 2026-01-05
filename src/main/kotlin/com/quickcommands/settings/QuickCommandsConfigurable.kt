package com.quickcommands.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

/**
 * Quick Commands settings page
 * Two tabs: Global Commands and Project Commands
 * 70%+ written with Claude
 */
class QuickCommandsConfigurable(private val project: Project) : Configurable {

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
            createCommandPanel(globalTableModel!!, "Visible in all projects")
        )

        // Tab 2: Project Commands
        projectTableModel = createTableModel()
        loadCommands(projectTableModel!!, ProjectCommandSettings.getInstance(project).commands)
        tabbedPane.addTab(
            "Project Commands",
            createCommandPanel(projectTableModel!!, "Visible only in '${project.name}' project")
        )

        mainPanel = tabbedPane
        return tabbedPane
    }

    private fun createTableModel(): DefaultTableModel {
        return object : DefaultTableModel(arrayOf("Name", "Command"), 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = true
        }
    }

    private fun loadCommands(model: DefaultTableModel, commands: List<CommandEntry>) {
        model.rowCount = 0
        commands.forEach { cmd ->
            model.addRow(arrayOf(cmd.name, cmd.command))
        }
    }

    private fun createCommandPanel(tableModel: DefaultTableModel, hint: String): JComponent {
        val table = JBTable(tableModel)
        table.columnModel.getColumn(0).preferredWidth = 150
        table.columnModel.getColumn(1).preferredWidth = 400

        val toolbar = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                tableModel.addRow(arrayOf("New Command", ""))
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
            .createPanel()

        val panel = JPanel(BorderLayout())
        panel.add(JBLabel("<html><i>$hint</i></html>"), BorderLayout.NORTH)
        panel.add(toolbar, BorderLayout.CENTER)

        return panel
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
            val name = model.getValueAt(i, 0) as? String ?: ""
            val command = model.getValueAt(i, 1) as? String ?: ""
            if (i >= commands.size || commands[i].name != name || commands[i].command != command) {
                return false
            }
        }
        return true
    }

    override fun apply() {
        // Save global commands
        val globalSettings = GlobalCommandSettings.getInstance()
        globalSettings.commands.clear()
        for (i in 0 until globalTableModel!!.rowCount) {
            val name = globalTableModel!!.getValueAt(i, 0) as? String ?: ""
            val command = globalTableModel!!.getValueAt(i, 1) as? String ?: ""
            if (name.isNotBlank() || command.isNotBlank()) {
                globalSettings.commands.add(CommandEntry(name, command))
            }
        }

        // Save project commands
        val projectSettings = ProjectCommandSettings.getInstance(project)
        projectSettings.commands.clear()
        for (i in 0 until projectTableModel!!.rowCount) {
            val name = projectTableModel!!.getValueAt(i, 0) as? String ?: ""
            val command = projectTableModel!!.getValueAt(i, 1) as? String ?: ""
            if (name.isNotBlank() || command.isNotBlank()) {
                projectSettings.commands.add(CommandEntry(name, command))
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
