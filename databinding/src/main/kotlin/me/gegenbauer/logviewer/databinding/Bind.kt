package me.gegenbauer.logviewer.databinding

// TODO use annotation to configure bindings
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Bind(val propertyType: String = "custom", val targetPropertyName: String = "")
