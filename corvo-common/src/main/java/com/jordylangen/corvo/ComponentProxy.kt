package com.jordylangen.corvo

interface ComponentProxy {

    fun <T> resolve(className: String) : T
}