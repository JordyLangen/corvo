package com.jordylangen.corvo

class Corvo(private val componentProxy: ComponentProxy) {

    fun resolveBinding(dependent: String) : Any? {
        return componentProxy.resolve(dependent)
    }

    inline fun <reified TDependent : Any, reified TDependency : Any> resolveBinding() : TDependency? {
        val className = TDependent::class.java.canonicalName
        return resolveBinding(className) as TDependency
    }
}