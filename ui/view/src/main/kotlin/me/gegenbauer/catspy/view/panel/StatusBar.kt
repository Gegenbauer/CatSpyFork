package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.strings.STRINGS
import java.awt.Color
import javax.swing.JComponent

interface StatusBar: TaskMonitor {
    var logStatus: LogStatus

    var memoryMonitorBar: JComponent

    var statusIcons: StatusIconsBar

    open class LogStatus(
        val backgroundColor: Color,
        val foregroundColor: Color,
        val status: String,
        val path: String
    ) {
        companion object {
            val NONE = LogStatus(Color.DARK_GRAY, Color.BLACK, "", STRINGS.ui.none)
        }
    }

    class LogStatusIdle(status: String, path: String = "") : LogStatus(
        Color.DARK_GRAY, Color.BLACK, status, path
    )

    class LogStatusRunning(status: String, path: String = "") : LogStatus(
        Color.GREEN, Color.WHITE, status, path
    )
}