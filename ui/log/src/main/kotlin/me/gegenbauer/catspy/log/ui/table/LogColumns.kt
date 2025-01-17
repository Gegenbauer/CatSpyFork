package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.model.LogcatItem.Companion.fgColor
import me.gegenbauer.catspy.log.ui.panel.BaseLogPanel
import me.gegenbauer.catspy.render.HtmlStringRenderer
import me.gegenbauer.catspy.render.Tag
import me.gegenbauer.catspy.render.TaggedStringBuilder
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.getMatchedList
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.border.AbstractBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn


internal val columnIndex = object : Column {
    override val name: String = "index"
    override val maxCharCount: Int = 8
    override val index: Int = 0

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : DefaultLogTableCellRenderer() {
            private val border = LineNumBorder(LogColorScheme.numLogSeparatorBG, 1)

            init {
                horizontalAlignment = JLabel.RIGHT
                verticalAlignment = JLabel.CENTER
            }

            override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
                border.color = LogColorScheme.numLogSeparatorBG
                label.border = border
                foreground = LogColorScheme.lineNumFG
            }
        }
    }
}

internal val columnTime = object : Column {
    override val name: String = "time"
    override val maxCharCount: Int = 20
    override val index: Int = 1

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRenderer()
    }
}

internal val columnPid = object : Column {
    override val name: String = "pid"
    override val maxCharCount: Int = 8
    override val index: Int = 2

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : HtmlLogCellRenderer() {
            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldPid
            }

            override fun getBoldColor(): Color {
                return LogColorScheme.pidFG
            }
        }
    }
}

internal val columnPackage = object : Column {
    override val name: String = "package"
    override val maxCharCount: Int = 35
    override val index: Int = 2

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : SearchableCellRenderer() {

            override fun addRenderItem(logTable: LogTable, row: Int, renderer: HtmlStringRenderer) {
                super.addRenderItem(logTable, row, renderer)
                logTable.tableModel.getLogFilter().filterPackage.getMatchedList(renderer.raw).forEach {
                    renderer.highlight(it.first, it.second, LogColorScheme.filteredBGs[0])
                    renderer.foreground(it.first, it.second, LogColorScheme.filteredFGs[0])
                }
            }

            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldPid
            }

            override fun getBoldColor(): Color {
                return LogColorScheme.pidFG
            }
        }
    }
}

internal val columnTid = object : Column {
    override val name: String = "tid"
    override val maxCharCount: Int = 8
    override val index: Int = 3

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : HtmlLogCellRenderer() {
            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldTid
            }

            override fun getBoldColor(): Color {
                return LogColorScheme.tidFG
            }
        }
    }
}

internal val columnLevel = object : Column {
    override val name: String = "level"
    override val maxCharCount: Int = 5
    override val index: Int = 4

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRenderer()
    }
}

internal val columnTag = object : Column {
    override val name: String = "tag"
    override val maxCharCount: Int = 25
    override val index: Int = 5

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : SearchableCellRenderer() {

            override fun addRenderItem(logTable: LogTable, row: Int, renderer: HtmlStringRenderer) {
                super.addRenderItem(logTable, row, renderer)
                logTable.tableModel.getLogFilter().filterTag.getMatchedList(renderer.raw).forEach {
                    renderer.highlight(it.first, it.second, LogColorScheme.filteredBGs[0])
                    renderer.foreground(it.first, it.second, LogColorScheme.filteredFGs[0])
                }
            }

            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldTag
            }

            override fun getBoldColor(): Color {
                return LogColorScheme.tagFG
            }
        }
    }
}

internal val columnMessage = object : Column {
    override val name: String = "message"
    override val maxCharCount: Int = 270
    override val index: Int = 6

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : SearchableCellRenderer() {
            override fun addRenderItem(logTable: LogTable, row: Int, renderer: HtmlStringRenderer) {
                super.addRenderItem(logTable, row, renderer)
                logTable.tableModel.getLogFilter().filterLog.getMatchedList(renderer.raw).forEach {
                    renderer.highlight(it.first, it.second, LogColorScheme.filteredBGs[0])
                    renderer.foreground(it.first, it.second, LogColorScheme.filteredFGs[0])
                }
            }
        }
    }
}

internal interface Column {
    val name: String
    val maxCharCount: Int
    val index: Int

    fun getCellRenderer(): DefaultTableCellRenderer

    fun configureColumn(table: JTable) {
        val tableColumn = if (table.columnCount <= index) {
            TableColumn(index).apply {
                table.addColumn(this)
            }
        } else {
            table.columnModel.getColumn(index)
        }
        tableColumn.cellRenderer = getCellRenderer()
        val fontMetrics = table.getFontMetrics(table.font)
        val width: Int = fontMetrics.charWidth('A') * maxCharCount + 2
        tableColumn.maxWidth = width
        tableColumn.width = width
        tableColumn.preferredWidth = width
    }
}

private class SimpleLogCellRenderer : DefaultLogTableCellRenderer() {
    init {
        horizontalAlignment = JLabel.LEFT
        verticalAlignment = JLabel.CENTER
    }

    override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        foreground = table.tableModel.getItemInCurrentPage(row).fgColor
    }
}

private open class SearchableCellRenderer: HtmlLogCellRenderer() {
    override fun addRenderItem(logTable: LogTable, row: Int, renderer: HtmlStringRenderer) {
        super.addRenderItem(logTable, row, renderer)
        val content = renderer.raw
        logTable.tableModel.searchFilterItem.getMatchedList(content).forEach {
            renderer.highlight(it.first, it.second, LogColorScheme.searchBG)
            renderer.foreground(it.first, it.second, LogColorScheme.searchFG)
        }
        logTable.tableModel.highlightFilterItem.getMatchedList(content).forEach {
            renderer.highlight(it.first, it.second, LogColorScheme.highlightBG)
            renderer.foreground(it.first, it.second, LogColorScheme.highlightFG)
        }
    }
}

private open class HtmlLogCellRenderer : DefaultLogTableCellRenderer() {
    init {
        horizontalAlignment = JLabel.LEFT
        verticalAlignment = JLabel.CENTER
    }

    override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        label.text = getRenderedContent(table, row, content)
    }

    protected fun getRenderedContent(logTable: LogTable, row: Int, content: String): String {
        val renderer = HtmlStringRenderer(content)
        val logItem = logTable.tableModel.getItemInCurrentPage(row)
        val foreground = logItem.fgColor
        renderer.foreground(0, content.length - 1, foreground)
        addRenderItem(logTable, row, renderer)

        if (renderer.isComplexityLow()) {
            if (renderer.getForegroundColor() != HtmlStringRenderer.INVALID_COLOR) {
                this.foreground = renderer.getForegroundColor()
            }
            if (renderer.getBackgroundColor() != HtmlStringRenderer.INVALID_COLOR) {
                this.background = renderer.getBackgroundColor()
            }
            return renderer.raw
        }

        return renderer.render()
    }

    open fun addRenderItem(logTable: LogTable, row: Int, renderer: HtmlStringRenderer) {
        if (shouldBold(logTable)) {
            renderer.foreground(0, renderer.raw.length - 1, getBoldColor())
            renderer.bold(0, renderer.raw.length - 1)
        }
    }

    open fun shouldBold(table: LogTable): Boolean = false

    open fun getBoldColor(): Color = Color.BLACK
}

private abstract class DefaultLogTableCellRenderer : DefaultTableCellRenderer() {
    private val emptyBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0)

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        col: Int
    ): Component {
        val logTable = table as LogTable
        val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col) as JLabel
        label.border = emptyBorder
        val content = value as? String ?: ""
        val logItem = logTable.tableModel.getItemInCurrentPage(row)
        background = logTable.getColumnBackground(logItem.num, row)
        render(table, label, row, col, content)
        return label
    }

    open fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        // Empty Implementation
    }
}

private class LineNumBorder(var color: Color, private val thickness: Int) : AbstractBorder() {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        if (width > 0) {
            g.color = color
            for (i in 1..thickness) {
                g.drawLine(width - i, y, width - i, height)
            }
        }
    }

    override fun getBorderInsets(c: Component): Insets {
        return getBorderInsets(c, Insets(0, 0, 0, thickness))
    }

    override fun getBorderInsets(c: Component?, insets: Insets): Insets {
        return insets.apply { set(0, 0, 0, thickness) }
    }

    override fun isBorderOpaque(): Boolean {
        return true
    }
}

fun LogTable.getRenderedContent(rows: List<Int>): String {
    val renderedContent = TaggedStringBuilder()
    renderedContent.addTag(Tag.HTML)
    renderedContent.addTag(Tag.BODY)
    val logFilterItem = tableModel.getLogFilter().filterLog
    var length = 0
    rows.forEachIndexed { index, row ->
        val logItem = tableModel.getItemInCurrentPage(row)
        val content = logItem.toLogLine()
        val renderer = HtmlStringRenderer(logItem.toLogLine())
        val matchedList = logFilterItem.getMatchedList(content)
        matchedList.forEach {
            renderer.highlight(it.first, it.second, LogColorScheme.filteredBGs[0])
            renderer.foreground(it.first, it.second, LogColorScheme.filteredFGs[0])
        }
        val foreground = logItem.fgColor
        renderer.foreground(0, content.length - 1, foreground)
        renderedContent.append(renderer.renderWithoutTags())
        if (index != rows.lastIndex) renderedContent.addSingleTag(Tag.LINE_BREAK)
        length += content.length
    }
    renderedContent.closeTag()
    renderedContent.closeTag()
    return renderedContent.build()
}

private fun LogTable.getColumnBackground(num: Int, row: Int): Color {
    val context = contexts.getContext(BaseLogPanel::class.java) ?: return LogColorScheme.logBG
    val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
    val isRowSelected = tableModel.selectedLogRows.contains(row)
    return if (bookmarkManager.isBookmark(num)) {
        if (isRowSelected) {
            LogColorScheme.bookmarkSelectedBG
        } else {
            LogColorScheme.bookmarkBG
        }
    } else if (isRowSelected) {
        LogColorScheme.selectedBG
    } else {
        LogColorScheme.logBG
    }
}

// TODO support generating columns from custom configuration
internal val fileLogColumns =
    listOf(columnIndex, columnTime, columnPid, columnTid, columnLevel, columnTag, columnMessage)

internal val deviceLogColumns =
    listOf(columnIndex, columnTime, columnPackage, columnTid, columnLevel, columnTag, columnMessage)
