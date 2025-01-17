package me.gegenbauer.catspy.log.ui.panel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.state.ListState
import me.gegenbauer.catspy.view.state.StatefulPanel
import java.awt.datatransfer.DataFlavor
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

class SplitLogPane(
    fullTableModel: LogTableModel,
    filteredTableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JSplitPane(), FocusListener, Context {

    var onFocusGained: (Boolean) -> Unit = {}

    val fullLogPanel = FullLogPanel(fullTableModel, this)
    val filteredLogPanel = FilteredLogPanel(filteredTableModel, this, fullLogPanel)
    private val filterStatefulPanel = StatefulPanel()
    private var rotation: Rotation = Rotation.ROTATION_LEFT_RIGHT
        set(value) {
            field = value
            changeRotation(value)
        }
    private val scope = MainScope()

    init {
        continuousLayout = true
        orientation = HORIZONTAL_SPLIT

        filterStatefulPanel.setContent(filteredLogPanel)
        filterStatefulPanel.listState = ListState.NORMAL
        scope.launch(Dispatchers.UI) {
            filteredTableModel.viewModel.filteredLogListState.collect {
                filterStatefulPanel.listState =
                    if (it == ListState.LOADING) ListState.LOADING else ListState.NORMAL
            }
        }

        add(fullLogPanel, LEFT)
        add(filterStatefulPanel, RIGHT)

        transferHandler = TableTransferHandler()
    }

    private fun changeRotation(rotation: Rotation) {
        remove(filterStatefulPanel)
        remove(fullLogPanel)
        when (rotation) {
            Rotation.ROTATION_LEFT_RIGHT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, LEFT)
                add(filterStatefulPanel, RIGHT)
                resizeWeight = SPLIT_WEIGHT
            }

            Rotation.ROTATION_TOP_BOTTOM -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, TOP)
                add(filterStatefulPanel, BOTTOM)
                resizeWeight = SPLIT_WEIGHT
            }

            Rotation.ROTATION_RIGHT_LEFT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, RIGHT)
                add(filterStatefulPanel, LEFT)
                resizeWeight = 1 - SPLIT_WEIGHT
            }

            Rotation.ROTATION_BOTTOM_TOP -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, BOTTOM)
                add(filterStatefulPanel, TOP)
                resizeWeight = 1 - SPLIT_WEIGHT
            }
        }
        SwingUtilities.updateComponentTreeUI(this)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        filteredLogPanel.setParent(this)
        fullLogPanel.setParent(this)
    }

    fun resetWithCurrentRotation() {
        changeRotation(rotation)
    }

    internal inner class TableTransferHandler : TransferHandler() {
        override fun canImport(info: TransferSupport): Boolean {
            return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        }

        override fun importData(info: TransferSupport): Boolean {
            GLog.d(TAG, "[importData] info = $info")
            info.takeIf { it.isDrop } ?: return false

            val fileList: MutableList<File> = mutableListOf()

            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                runCatching {
                    val data = info.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                    data?.mapNotNull { it as? File }?.forEach { fileList.add(it) }
                }.onFailure {
                    GLog.e(TAG, "[importData]", it)
                }
            }

            fileList.takeIf { it.isNotEmpty() } ?: return false
            val os = currentPlatform
            GLog.d(TAG, "os:$os, drop:${info.dropAction},sourceDrop:${info.sourceDropActions},userDrop:${info.userDropAction}")

            val logMainUI = contexts.getContext(BaseLogPanel::class.java)
            logMainUI ?: return false

            val options = listOf<Pair<String, (List<File>) -> Unit>>(
                STRINGS.ui.open to { files ->
                    files.firstOrNull()?.let { logMainUI.openFile(it.absolutePath) }
                },
                STRINGS.ui.cancel to { GLog.d(TAG, "[onDragLogFile] select cancel") }
            )
            val value = JOptionPane.showOptionDialog(
                this@SplitLogPane, STRINGS.ui.dragLogFileWarning,
                "",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options.map { it.first }.toTypedArray(),
                STRINGS.ui.append
            )
            options[value].second.invoke(fileList)
            return true
        }
    }

    override fun destroy() {
        super.destroy()
        filteredLogPanel.destroy()
        fullLogPanel.destroy()
        scope.cancel()
    }

    override fun focusGained(e: FocusEvent) {
        onFocusGained.invoke(e.source == filteredLogPanel.table)
    }

    override fun focusLost(e: FocusEvent) {
        // do nothing
    }

    companion object {
        private const val TAG = "SplitLogPane"
        private const val SPLIT_WEIGHT = 0.7
    }

}

fun Rotation.nextRotation(): Rotation {
    return when (this) {
        Rotation.ROTATION_LEFT_RIGHT -> {
            Rotation.ROTATION_TOP_BOTTOM
        }

        Rotation.ROTATION_TOP_BOTTOM -> {
            Rotation.ROTATION_RIGHT_LEFT
        }

        Rotation.ROTATION_RIGHT_LEFT -> {
            Rotation.ROTATION_BOTTOM_TOP
        }

        Rotation.ROTATION_BOTTOM_TOP -> {
            Rotation.ROTATION_LEFT_RIGHT
        }
    }
}