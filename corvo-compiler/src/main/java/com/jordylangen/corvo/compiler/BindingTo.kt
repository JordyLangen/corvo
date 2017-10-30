package com.jordylangen.corvo.compiler

import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

data class BindingTo(
        var dependent: String,
        var dependencyName: String,
        var dependencyType: TypeMirror,
        var moduleName: String,
        var moduleType: TypeMirror)