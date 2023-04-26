package me.gegenbauer.logviewer.ui.dialog

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogPanel
import me.gegenbauer.logviewer.utils.Utils
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel

class LogTableDialog (private val parent: MainUI, private val logPanel: LogPanel) : JFrame("FullLog") {
    private var frameX = 0
    private var frameY = 0
    private var frameWidth = 1280
    private var frameHeight = 720
    private var frameExtendedState = java.awt.Frame.MAXIMIZED_BOTH

    init {
        val img = loadIcon<DerivableImageIcon>("logo.png")
        iconImage = img.image

        defaultCloseOperation = DISPOSE_ON_CLOSE
        setLocation(frameX, frameY)
        setSize(frameWidth, frameHeight)
        val panel = JPanel(BorderLayout())
        panel.add(this.logPanel, BorderLayout.CENTER)
        contentPane.add(panel)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                GLog.i(TAG, "exit table dialog")
                parent.attachLogPanel(this@LogTableDialog.logPanel)
            }
        })

        Utils.installKeyStrokeEscClosing(this)
    }

    companion object {
        private const val TAG = "LogTableDialog"
    }
}