package me.gegenbauer.catspy.log.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.log.model.LogFilter
import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.log.model.LogcatRealTimeFilter
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskListener
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.swing.event.TableModelEvent
import kotlin.concurrent.write

abstract class BaseLogcatRepository(
    private val updateUITask: PeriodicTask,
) : LogRepository, TaskListener {

    override var fullMode: Boolean = false
        set(value) {
            if (field != value) {
                onFilterUpdate()
            }
            field = value
        }
    override var bookmarkMode: Boolean = false
        set(value) {
            if (field != value) {
                onFilterUpdate()
            }
            field = value
        }
    override var logFilter: LogFilter<LogcatLogItem> = LogcatRealTimeFilter.emptyRealTimeFilter
        set(value) {
            if (field != value) {
                onFilterUpdate()
            }
            field = value
        }
    override var selectedRow: Int = 0

    private val logItems = ArrayList<LogcatLogItem>(10000)
    private val cacheItems = ArrayList<LogcatLogItem>(10000)
    private val logChangeListeners = mutableListOf<LogRepository.LogChangeListener>()
    private val cacheItemLock = ReentrantReadWriteLock()
    private val logItemLock = ReentrantReadWriteLock()
    private val scope = MainScope()

    init {
        registerTaskListener()
    }

    private fun registerTaskListener() {
        updateUITask.addListener(this)
    }

    override fun onLogItemReceived(logItem: LogcatLogItem) {
        onItemInsertFromInit(logItem)
    }

    override fun onItemInsertFromInit(logItem: LogcatLogItem) {
        addLogItem(logItem)
    }

    override fun onLogCleared() {
        clear()
    }

    override fun <R> accessCacheItems(visitor: (MutableList<LogcatLogItem>) -> R): R {
        return cacheItemLock.write { visitor(cacheItems) }
    }

    override fun <R> accessLogItems(visitor: (MutableList<LogcatLogItem>) -> R): R {
        return logItemLock.write { visitor(logItems) }
    }

    override fun onRepeat(task: Task) {
        super.onRepeat(task)
        scope.launch(Dispatchers.UI) {
            processCacheForUIUpdate()
        }
    }

    protected open fun processCacheForUIUpdate() {
        accessLogItems { logItems ->
            accessCacheItems { cacheItems ->
                if (cacheItems.isEmpty()) return@accessCacheItems
                val rowBeforeAdd = logItems.size
                logItems.addAll(cacheItems.filter(::filterRule))
                notifyLogItemInsert(rowBeforeAdd, rowBeforeAdd + cacheItems.size - 1)
                cacheItems.clear()
            }
        }
    }

    abstract fun filterRule(item: LogcatLogItem): Boolean

    protected open fun addLogItem(logItem: LogcatLogItem) {
        accessCacheItems { it.add(logItem) }
    }

    protected fun notifyLogItemInsert(startRow: Int, endRow: Int) {
        logChangeListeners.forEach {
            it.onLogChanged(
                LogRepository.LogChangeEvent(
                    TableModelEvent.INSERT,
                    startRow,
                    endRow,
                )
            )
        }
    }

    protected fun notifyLogDataSetChange() {
        logChangeListeners.forEach {
            it.onLogChanged(
                LogRepository.LogChangeEvent(
                    TableModelEvent.UPDATE,
                    0,
                    Int.MAX_VALUE,
                )
            )
        }
    }

    protected fun notifyLogItemRemove(startRow: Int, endRow: Int) {
        logChangeListeners.forEach {
            it.onLogChanged(
                LogRepository.LogChangeEvent(
                    TableModelEvent.DELETE,
                    startRow,
                    endRow,
                )
            )
        }
    }

    override fun onItemInsertFromFilterUpdate(logItem: LogcatLogItem) {
        //
    }

    override fun onFilterUpdate() {
        //
    }

    override fun cancelFilterUpdate() {
        //
    }

    override fun clear() {
        cacheItemLock.write {
            logItems.clear()
            notifyLogItemRemove(0, Int.MAX_VALUE)
        }
    }

    override fun addLogChangeListener(listener: LogRepository.LogChangeListener) {
        logChangeListeners.add(listener)
    }

    override fun removeLogChangeListener(listener: LogRepository.LogChangeListener) {
        logChangeListeners.remove(listener)
    }

    override fun getLogCount(): Int {
        return accessLogItems { logItems.size }
    }
}