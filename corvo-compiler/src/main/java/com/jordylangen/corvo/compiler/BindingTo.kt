package com.jordylangen.corvo.compiler

import kotlin.reflect.KClass

data class BindingTo(var dependent: KClass<*>, var dependency: KClass<*>, var module: KClass<*>)