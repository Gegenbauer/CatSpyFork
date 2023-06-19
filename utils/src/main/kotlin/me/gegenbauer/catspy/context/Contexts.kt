package me.gegenbauer.catspy.context

import java.lang.ref.WeakReference

class Contexts {
    private val contexts = mutableMapOf<Int, WeakReference<Context>>()

    fun putContext(context: Context): Contexts {
        contexts[context.getId()] = WeakReference(context)
        return this
    }

    fun removeContext(context: Context): Contexts {
        contexts.remove(context.getId())
        return this
    }

    fun getContext(contextId: Int): Context? {
        return contexts[contextId]?.get()
    }

    fun <T: Context> getContext(clazz: Class<T>): T? {
        contexts.values.forEach {
            val context = it.get()
            if (context != null && clazz.isInstance(context)) {
                return context as? T
            }
        }
        return null
    }

    fun set(contexts: Contexts): Contexts {
        this.contexts.clear()
        this.contexts.putAll(contexts.contexts)
        return this
    }

    fun clone(): Contexts {
        val clone = Contexts()
        clone.contexts.putAll(contexts)
        return clone
    }

    companion object {
        val default: Contexts
            get() = Contexts()
    }
}