package me.gegenbauer.logviewer.databinding.adapter.component

import me.gegenbauer.logviewer.databinding.adapter.Disposable

interface DisposableAdapter {
    fun dispose() {
        this::class.java.methods
            .filter { it.isAnnotationPresent(Disposable::class.java) }
            .forEach { it.invoke(this) }
    }
}