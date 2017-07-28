package com.jordylangen.corvo

import com.jordylangen.corvo.annotations.BindingDependencyResolver

class Corvo(val dependencyResolver: BindingDependencyResolver) {

    fun resolveBinding(dependent: String) : Any? {
        return dependencyResolver.resolve(dependent)
    }

    inline fun <reified TDependent : Any, reified TDependency : Any> resolveBinding() : TDependency? {
        val className = TDependent::class.java.canonicalName
        return dependencyResolver.resolve(className)
    }
}