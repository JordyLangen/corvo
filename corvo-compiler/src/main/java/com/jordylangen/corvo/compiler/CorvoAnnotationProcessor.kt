package com.jordylangen.corvo.compiler

import com.jordylangen.corvo.annotations.BindsTo
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import dagger.Component
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import javax.lang.model.element.*


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

        val dependencies = mutableListOf<KClass<*>>()
        val modules = mutableListOf<KClass<*>>()

        for (element in roundEnv.getElementsAnnotatedWith(BindsTo::class.java)) {

            val annotationMirrors = element.annotationMirrors.filter { mirror ->
                mirror.annotationType.toString() == BindsTo::class.qualifiedName
            }

            for (annotationMirror in annotationMirrors) {
                for (entry in annotationMirror.elementValues.entries) {
                    if (entry.key.simpleName.toString() == "dependency") {
                        val className = entry.value.value.toString()
                        val klass = Class.forName(className).kotlin

                        if (!dependencies.contains(klass)) {
                            dependencies.add(klass)
                        }
                    }
                    else if (entry.key.simpleName.toString() == "module") {
                        val className = entry.value.value.toString()
                        val klass = Class.forName(className).kotlin

                        if (!modules.contains(klass)) {
                            modules.add(klass)
                        }
                    }
                }
            }
        }

        val componentAnnotation = AnnotationSpec.builder(Component::class.java)
                .addMember("modules", modules.map { "${it.qualifiedName}.class" }.joinToString(separator = ", ", prefix = "{ ", postfix = " }"))
                .build()

        val methods = dependencies.map { dependency ->
            MethodSpec.methodBuilder("resolve${dependency.simpleName}")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(dependency.java)
                    .build()
        }

        val typeSpec = TypeSpec.interfaceBuilder("CorvoComponent")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation)
                .addMethods(methods)
                .build()

        val file = JavaFile.builder("com.jordylangen.corvo", typeSpec)
                .build()

        file.writeTo(filer)

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