package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.model.LogItem
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskManager
import java.io.File

/**
 * Collecting logs from a source and notifying observers
 */
interface LogCollector<T: LogItem>: LogObservable<T> {
    val taskManager: TaskManager
    val collectorTask: Task

    fun startCollecting()

    fun pauseCollecting()

    fun resumeCollecting()

    fun stopCollecting()

    fun isCollecting(): Boolean

    fun getLogFile(): File?

    fun clear()

    fun destroy()
}