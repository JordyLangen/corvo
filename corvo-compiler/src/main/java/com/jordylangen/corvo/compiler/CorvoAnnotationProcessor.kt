package com.jordylangen.corvo.compiler

import com.jordylangen.corvo.annotations.BindsTo
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dagger.Component
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class CorvoAnnotationProcessor : AbstractProcessor() {

    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private var didProcess = false

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
        messager = processingEnv.messager
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (didProcess) {
            return true
        }

        val componentAnnotation = AnnotationSpec.builder(Component::class.java)
                .addMember("modules", "{ jordylangen.corvo.example.SampleModule.class }")
                .build()

        val typeSpec = TypeSpec.interfaceBuilder("CorvoComponent")
                .addAnnotation(componentAnnotation)
                .build()

        val file = JavaFile.builder("com.jordylangen.corvo", typeSpec)
                .build()

        file.writeTo(filer)

        /*
        for (element in roundEnv.getElementsAnnotatedWith(BindsTo::class.java)) {
            println(element.simpleName)
        }
        */

        didProcess = true

        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(BindsTo::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
}