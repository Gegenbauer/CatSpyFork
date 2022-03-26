package com.blogspot.kotlinstudy.lognote

import java.io.IOException
import java.util.*


class AdbManager private constructor(){
    var mPrefix: String =""
    var mAdbCmd = "adb"
    var mLogSavePath:String = "."
    var mTargetDevice: String = ""
    var mDevices = ArrayList<String>()
    val mEventListeners = ArrayList<AdbEventListener>()

    companion object {
        const val EVENT_NONE = 0
        const val EVENT_SUCCESS = 1
        const val EVENT_FAIL = 2

        const val CMD_CONNECT = 1
        const val CMD_GET_DEVICES = 2
        const val CMD_LOGCAT = 3
        const val CMD_DISCONNECT = 4

        private val mInstance: AdbManager = AdbManager()

        fun getInstance(): AdbManager {
            return mInstance
        }
    }

    fun getDevices() {
        execute(makeExecuter(CMD_GET_DEVICES))
    }

    fun connect() {
        if (mTargetDevice.isEmpty()) {
            println("Target device is not selected")
            return
        }

        execute(makeExecuter(CMD_CONNECT))
    }

    fun disconnect() {
        execute(makeExecuter(CMD_DISCONNECT))
    }

    fun startLogcat() {
        execute(makeExecuter(CMD_LOGCAT))
    }

    fun stop() {
        println("Stop all processes")
        mProcessLogcat?.destroy()
        mProcessLogcat = null
        mCurrentExecuter?.interrupt()
        mCurrentExecuter = null
    }

    fun addEventListener(eventListener:AdbEventListener) {
        mEventListeners.add(eventListener)
    }

    private fun sendEvent(event: AdbEvent) {
        for (listener in mEventListeners) {
            listener.changedStatus(event)
        }
    }

    private var mCurrentExecuter:Thread? = null
    var mProcessLogcat:Process? = null
    private fun execute(cmd:Runnable?) {
        cmd?.run()
    }

    private fun makeExecuter(cmdNum:Int) :Runnable? {
        var executer:Runnable? = null
        when (cmdNum) {
            CMD_CONNECT -> executer = Runnable {
                run {
                    val cmd = "$mAdbCmd connect $mTargetDevice"
                    val runtime = Runtime.getRuntime()
                    val scanner = try {
                        val process = runtime.exec(cmd)
                        Scanner(process.inputStream)
                    } catch (e:IOException) {
                        println("Failed run $cmd")
                        val adbEvent = AdbEvent(CMD_CONNECT, EVENT_FAIL)
                        sendEvent(adbEvent)
                        return@run
                    }

                    var line:String
                    var isSuccess = false
                    while (scanner.hasNextLine()) {
                        line = scanner.nextLine()
                        if (line.contains("connected to")) {
                            println("Success connect to $mTargetDevice")
                            val adbEvent = AdbEvent(CMD_CONNECT, EVENT_SUCCESS)
                            sendEvent(adbEvent)
                            isSuccess = true
                            break
                        }
                    }

                    if (!isSuccess) {
                        println("Failed connect to $mTargetDevice")
                        val adbEvent = AdbEvent(CMD_CONNECT, EVENT_FAIL)
                        sendEvent(adbEvent)
                    }
                }
            }

            CMD_GET_DEVICES -> executer = Runnable {
                run {
                    mDevices.clear()

                    val cmd = "$mAdbCmd devices"
                    val runtime = Runtime.getRuntime()
                    val scanner = try {
                        val process = runtime.exec(cmd)
                        Scanner(process.inputStream)
                    } catch (e:IOException) {
                        println("Failed run $cmd")
                        val adbEvent = AdbEvent(CMD_GET_DEVICES, EVENT_FAIL)
                        sendEvent(adbEvent)
                        return@run
                    }

                    var line:String
                    while (scanner.hasNextLine()) {
                        line = scanner.nextLine()
                        if (line.contains("List of devices")) {
                            continue
                        }
                        val textSplited = line.trim().split(Regex("\\s+"))
                        if (textSplited.size >= 2) {
                            println("device : ${textSplited[0]}")
                            mDevices.add(textSplited[0])
                        }
                    }
                    val adbEvent = AdbEvent(CMD_GET_DEVICES, EVENT_SUCCESS)
                    sendEvent(adbEvent)
                }
            }
            CMD_LOGCAT -> executer = Runnable {
                run {
                    mProcessLogcat?.destroy()
                    val cmd = "$mAdbCmd -s $mTargetDevice logcat -v threadtime"
                    println("Start : $cmd")
                    val runtime = Runtime.getRuntime()
                    try {
                        mProcessLogcat = runtime.exec(cmd)
                        val processExitDetector = ProcessExitDetector(mProcessLogcat!!)
                        processExitDetector.addProcessListener(object : ProcessListener {
                            override fun processFinished(process: Process?) {
                                println("The subprocess has finished.")
                                mProcessLogcat?.inputStream?.close()
                            }
                        })
                        processExitDetector.start()
                    } catch (e:IOException) {
                        println("Failed run $cmd")
                        mProcessLogcat = null
                        return@run
                    }
                    println("End : $cmd")
                }
            }
            CMD_DISCONNECT -> executer = Runnable {
                run {
                    val cmd = "$mAdbCmd disconnect"
                    val runtime = Runtime.getRuntime()
                    try {
                        runtime.exec(cmd)
                    } catch (e: IOException) {
                        println("Failed run $cmd")
                        val adbEvent = AdbEvent(CMD_DISCONNECT, EVENT_FAIL)
                        sendEvent(adbEvent)
                        return@run
                    }

                    val adbEvent = AdbEvent(CMD_DISCONNECT, EVENT_SUCCESS)
                    sendEvent(adbEvent)
                }
            }
        }

        return executer
    }

    interface AdbEventListener {
        fun changedStatus(event:AdbEvent)
    }

    class AdbEvent(c:Int, e:Int) {
        val cmd = c
        val event = e
    }

    interface ProcessListener : EventListener {
        fun processFinished(process: Process?)
    }

    class ProcessExitDetector(process: Process) : Thread() {
        var process: Process? = null
        private val listeners: MutableList<ProcessListener> = ArrayList<ProcessListener>()
        override fun run() {
            try {
                process!!.waitFor()
                for (listener in listeners) {
                    listener.processFinished(process)
                }
            } catch (e: InterruptedException) {
            }
        }

        fun addProcessListener(listener: ProcessListener) {
            listeners.add(listener)
        }

        fun removeProcessListener(listener: ProcessListener) {
            listeners.remove(listener)
        }

        init {
            try {
                process.exitValue()
                throw IllegalArgumentException("The process is already ended")
            } catch (exc: IllegalThreadStateException) {
                this.process = process
            }
        }
    }
}