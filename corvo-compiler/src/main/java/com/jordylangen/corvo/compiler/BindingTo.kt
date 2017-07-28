package com.jordylangen.corvo.compiler

import kotlin.reflect.KClass

data class BindingTo(var dependent: String, var dependency: KClass<*>, var module: KClass<*>)