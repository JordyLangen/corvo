package com.jordylangen.corvo.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class BindsTo(val dependency: KClass<*>, val module: KClass<*>)