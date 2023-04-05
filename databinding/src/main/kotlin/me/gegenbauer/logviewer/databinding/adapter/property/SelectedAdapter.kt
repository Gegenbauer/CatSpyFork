package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import java.awt.event.ItemListener

interface SelectedAdapter : ComponentAdapter {
    val selectedChangeListener: ItemListener

    fun updateSelectedStatus(value: Boolean?)

    fun observeSelectedStatusChange(observer: (Boolean?) -> Unit)

    @Disposable
    fun removeSelectedChangeListener()
}