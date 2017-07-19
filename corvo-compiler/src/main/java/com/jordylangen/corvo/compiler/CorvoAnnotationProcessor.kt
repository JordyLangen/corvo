package com.jordylangen.corvo.compiler

import com.jordylangen.corvo.annotations.BindsTo
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import dagger.Component
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import com.sun.deploy.util.SystemUtils.getSimpleName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue


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
                        println(entry.value.toString())
                    }
                }
            }
        }

        val componentAnnotation = AnnotationSpec.builder(Component::class.java)
                .addMember("modules", modules.map { it.qualifiedName }.joinToString(separator = ", ", prefix = "{ ", postfix = " }"))
                .build()

        val methods = dependencies.map { dependency ->
            MethodSpec.methodBuilder("resolve${dependency.simpleName}")
                    .build()
        }

        val typeSpec = TypeSpec.interfaceBuilder("CorvoComponent")
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