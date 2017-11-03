package com.jordylangen.corvo.annotations

interface ComponentProxy {

    fun <T> resolve(className: String) : T
}