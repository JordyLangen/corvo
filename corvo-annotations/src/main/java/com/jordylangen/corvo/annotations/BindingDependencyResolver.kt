package com.jordylangen.corvo.annotations

interface BindingDependencyResolver {

    fun <T> resolve(className: String) : T
}