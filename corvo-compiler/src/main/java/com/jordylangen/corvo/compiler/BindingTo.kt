package com.jordylangen.corvo.compiler

import javax.lang.model.element.TypeElement

data class BindingTo(
        var dependent: TypeElement,
        var dependency: TypeElement,
        var module: TypeElement)