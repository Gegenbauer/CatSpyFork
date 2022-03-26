package com.blogspot.kotlinstudy.lognote

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ConfigManager private constructor() {
    companion object {
        private const val CONFIG_FILE = "lognote.xml"
        const val ITEM_FRAME_X = "FRAME_X"
        const val ITEM_FRAME_Y = "FRAME_Y"
        const val ITEM_FRAME_WIDTH = "FRAME_WIDTH"
        const val ITEM_FRAME_HEIGHT = "FRAME_HEIGHT"
        const val ITEM_FRAME_EXTENDED_STATE = "FRAME_EXTENDED_STATE"
        const val ITEM_ROTATION = "ROTATION"
        const val ITEM_DIVIDER_LOCATION = "DIVIDER_LOCATION"
        const val ITEM_LAST_DIVIDER_LOCATION = "LAST_DIVIDER_LOCATION"

        const val ITEM_LANG = "LANG"

        const val ITEM_SHOW_LOG = "SHOW_LOG_"
        const val COUNT_SHOW_LOG = 20
        const val ITEM_SHOW_TAG = "SHOW_TAG_"
        const val COUNT_SHOW_TAG = 10

        const val ITEM_HIGHLIGHT_LOG = "HIGHLIGHT_LOG_"
        const val COUNT_HIGHLIGHT_LOG = 10

        const val ITEM_SHOW_LOG_CHECK = "SHOW_LOG_CHECK"
        const val ITEM_SHOW_TAG_CHECK = "SHOW_TAG_CHECK"
        const val ITEM_SHOW_PID_CHECK = "SHOW_PID_CHECK"
        const val ITEM_SHOW_TID_CHECK = "SHOW_TID_CHECK"

        const val ITEM_HIGHLIGHT_LOG_CHECK = "HIGHLIGHT_LOG_CHECK"

        const val ITEM_LOG_LEVEL = "LOG_LEVEL"

        const val ITEM_ADB_DEVICE = "ADB_DEVICE"
        const val ITEM_ADB_CMD = "ADB_CMD"
        const val ITEM_ADB_LOG_SAVE_PATH = "ADB_LOG_SAVE_PATH"
        const val ITEM_ADB_PREFIX = "ADB_PREFIX"

        const val ITEM_FONT_NAME = "FONT_NAME"
        const val ITEM_FONT_SIZE = "FONT_SIZE"
        const val ITEM_VIEW_FULL = "VIEW_FULL"
        const val ITEM_FILTER_INCREMENTAL = "FILTER_INCREMENTAL"

        const val ITEM_SCROLLBACK = "SCROLLBACK"
        const val ITEM_SCROLLBACK_SPLIT_FILE = "SCROLLBACK_SPLIT_FILE"
        const val ITEM_MATCH_CASE = "MATCH_CASE"

        const val ITEM_FILTERS_TITLE = "FILTERS_TITLE_"
        const val ITEM_FILTERS_FILTER = "FILTERS_FILTER_"
        const val ITEM_FILTERS_TABLEBAR = "FILTERS_TABLEBAR_"

        const val ITEM_CMDS_TITLE = "CMDS_TITLE_"
        const val ITEM_CMDS_CMD = "CMDS_CMD_"
        const val ITEM_CMDS_TABLEBAR = "CMDS_TABLEBAR_"

        const val ITEM_COLOR_MANAGER = "COLOR_MANAGER_"

        const val ITEM_RETRY_ADB = "RETRY_ADB"

        private val mInstance: ConfigManager = ConfigManager()

        fun getInstance(): ConfigManager {
            return mInstance
        }
    }
    private val mProperties = Properties()

    private fun setDefaultConfig() {
        mProperties[ITEM_LOG_LEVEL] = MainUI.VERBOSE
        mProperties[ITEM_SHOW_LOG_CHECK] = "true"
        mProperties[ITEM_SHOW_TAG_CHECK] = "true"
        mProperties[ITEM_SHOW_PID_CHECK] = "true"
        mProperties[ITEM_SHOW_TID_CHECK] = "true"
        mProperties[ITEM_HIGHLIGHT_LOG_CHECK] = "true"
    }

    fun loadConfig() {
        var fileInput: FileInputStream? = null

        try {
            fileInput = FileInputStream(CONFIG_FILE)
            mProperties.loadFromXML(fileInput)
        } catch (ex: Exception) {
            ex.printStackTrace()
            setDefaultConfig()
        } finally {
            if (null != fileInput) {
                try {
                    fileInput.close()
                } catch (ex: IOException) {
                }
            }
        }
    }

    fun saveConfig() {
        var fileOutput: FileOutputStream? = null
        try {
            fileOutput = FileOutputStream(CONFIG_FILE)
            mProperties.storeToXML(fileOutput, "")
        } finally {
            if (null != fileOutput) {
                try {
                    fileOutput.close()
                } catch (ex: IOException) {
                }
            }
        }
    }

    fun saveItem(key: String, value: String) {
        loadConfig()
        setItem(key, value)
        saveConfig()
    }

    fun saveItems(keys: Array<String>, values: Array<String>) {
        loadConfig()
        setItems(keys, values)
        saveConfig()
    }

    fun getItem(key: String): String? {
        return mProperties[key] as String?
    }

    fun setItem(key: String, value: String) {
        mProperties[key] = value
    }

    fun setItems(keys: Array<String>, values: Array<String>) {
        if (keys.size != values.size) {
            println("saveItem : size not match ${keys.size}, ${values.size}")
            return
        }
        for (idx in keys.indices) {
            mProperties[keys[idx]] = values[idx]
        }
    }

    fun removeConfigItem(key: String) {
        mProperties.remove(key)
    }

    fun saveFontColors(family: String, size: Int) {
        loadConfig()

        mProperties[ITEM_FONT_NAME] = family
        mProperties[ITEM_FONT_SIZE] = size.toString()
        ColorManager.getInstance().putConfig()

        saveConfig()
    }

    fun loadFilters() : ArrayList<CustomListManager.CustomElement> {
        val filters = ArrayList<CustomListManager.CustomElement>()

        var title: String?
        var filter: String?
        var check: String?
        var tableBar: Boolean
        for (i in 0 until FiltersManager.MAX_FILTERS) {
            title = mProperties[ITEM_FILTERS_TITLE + i] as? String
            if (title == null) {
                break
            }
            filter = mProperties[ITEM_FILTERS_FILTER + i] as? String
            if (filter == null) {
                filter = "null"
            }

            check = mProperties[ITEM_FILTERS_TABLEBAR + i] as? String
            if (!check.isNullOrEmpty()) {
                tableBar = check.toBoolean()
            } else {
                tableBar = false
            }
            filters.add(CustomListManager.CustomElement(title, filter, tableBar))
        }

        return filters
    }

    fun saveFilters(filters : ArrayList<CustomListManager.CustomElement>) {
        loadConfig()

        var nCount = filters.size
        if (nCount > FiltersManager.MAX_FILTERS) {
            nCount = FiltersManager.MAX_FILTERS
        }

        for (i in 0 until FiltersManager.MAX_FILTERS) {
            val title = mProperties[ITEM_CMDS_TITLE + i] as? String
            if (title == null) {
                break
            }
            mProperties.remove(ITEM_FILTERS_TITLE + i)
            mProperties.remove(ITEM_FILTERS_FILTER + i)
            mProperties.remove(ITEM_FILTERS_TABLEBAR + i)
        }

        for (i in 0 until nCount) {
            mProperties[ITEM_FILTERS_TITLE + i] = filters[i].mTitle
            mProperties[ITEM_FILTERS_FILTER + i] = filters[i].mValue
            mProperties[ITEM_FILTERS_TABLEBAR + i] = filters[i].mTableBar.toString()
        }

        saveConfig()
        return
    }

    fun loadCmds() : ArrayList<CustomListManager.CustomElement> {
        val cmds = ArrayList<CustomListManager.CustomElement>()

        var title: String?
        var cmd: String?
        var check: String?
        var tableBar: Boolean
        for (i in 0 until CmdsManager.MAX_CMDS) {
            title = mProperties[ITEM_CMDS_TITLE + i] as? String
            if (title == null) {
                break
            }
            cmd = mProperties[ITEM_CMDS_CMD + i] as? String
            if (cmd == null) {
                cmd = "null"
            }

            check = mProperties[ITEM_CMDS_TABLEBAR + i] as? String
            if (!check.isNullOrEmpty()) {
                tableBar = check.toBoolean()
            } else {
                tableBar = false
            }
            cmds.add(CustomListManager.CustomElement(title, cmd, tableBar))
        }

        return cmds
    }

    fun saveCmds(cmds : ArrayList<CustomListManager.CustomElement>) {
        loadConfig()

        var nCount = cmds.size
        if (nCount > CmdsManager.MAX_CMDS) {
            nCount = CmdsManager.MAX_CMDS
        }

        for (i in 0 until CmdsManager.MAX_CMDS) {
            val title = mProperties[ITEM_CMDS_TITLE + i] as? String
            if (title == null) {
                break
            }
            mProperties.remove(ITEM_CMDS_TITLE + i)
            mProperties.remove(ITEM_CMDS_CMD + i)
            mProperties.remove(ITEM_CMDS_TABLEBAR + i)
        }

        for (i in 0 until nCount) {
            mProperties[ITEM_CMDS_TITLE + i] = cmds[i].mTitle
            mProperties[ITEM_CMDS_CMD + i] = cmds[i].mValue
            mProperties[ITEM_CMDS_TABLEBAR + i] = cmds[i].mTableBar.toString()
        }

        saveConfig()
        return
    }
}
