package com.jordylangen.corvo.compiler

import com.jordylangen.corvo.annotations.BindingDependencyResolver
import com.jordylangen.corvo.annotations.BindsTo
import com.squareup.javapoet.*
import dagger.Component
import java.lang.IllegalArgumentException
import java.util.HashMap
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror


class CorvoAnnotationProcessor : AbstractProcessor() {

    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private var processedElements = mutableListOf<Element>()

    companion object {
        private const val PACKAGE_NAME = "com.jordylangen.corvo"
        private const val DAGGER_PROPERTY_MODULES = "modules"
        private const val BINDS_TO_PROPERTY_DEPENDENCY = "dependency"
        private const val BINDS_TO_PROPERTY_MODULE = "module"
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
        messager = processingEnv.messager
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val bindings = mutableListOf<BindingTo>()
        val elements = roundEnv.getElementsAnnotatedWith(BindsTo::class.java)
                .filter { !processedElements.contains(it) }

        if (elements.isEmpty()) {
            return true
        }

        for (element in elements) {
            bindings.addAll(resolveBindings(element))
            processedElements.add(element)
        }

        val dependencies = bindings.map { binding -> binding.dependencyType }.distinct()
        val modules = bindings.map { binding -> binding.moduleType }.distinct()

        val componentAnnotation = AnnotationSpec.builder(Component::class.java)
                .addMember(DAGGER_PROPERTY_MODULES, modules.map { "${it.toString()}.class" }.joinToString(separator = ", ", prefix = "{ ", postfix = " }"))
                .build()

        val componentMethods = dependencies.map { dependency ->
            MethodSpec.methodBuilder("resolve${dependency.toString().split('.')[dependency.toString().split('.').size - 1]}")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeName.get(dependency))
                    .build()
        }

        val corvoComponentSpec = TypeSpec.interfaceBuilder("CorvoComponent")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation)
                .addMethods(componentMethods)
                .build()

        val componentFile = JavaFile.builder(PACKAGE_NAME, corvoComponentSpec)
                .build()

        componentFile.writeTo(filer)

        val proxyConstructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(PACKAGE_NAME, "CorvoComponent"), "component")
                .addStatement("this.component = component")
                .addStatement("this.bindings = new HashMap<String, String>()")

        for (binding in bindings) {
            proxyConstructorBuilder.addStatement("this.bindings.put(\"${binding.dependent}\", \"${binding.dependencyType}\")")
        }

        val resolveMethodBuilder = MethodSpec.methodBuilder("resolve")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .addParameter(String::class.java, "className")
                .addStatement("String dependency = bindings.get(className)")
                .addStatement("if (!bindings.containsKey(dependency)) { return null; }")
                .returns(TypeVariableName.get("T"))

        for (binding in bindings) {
            resolveMethodBuilder.addStatement("if (dependency.equals(\"${binding.dependencyType}\")) { return (T) component.resolve${binding.dependencyType.toString().split('.')[binding.dependencyType.toString().split('.').size - 1]}(); }")
        }

        resolveMethodBuilder.addStatement("return null") // TODO throw an exception

        val proxySpec = TypeSpec.classBuilder("CorvoBindingDependencyResolver")
                .addField(ClassName.get(PACKAGE_NAME, "CorvoComponent"), "component", Modifier.PRIVATE, Modifier.FINAL)
                .addField(ParameterizedTypeName.get(HashMap::class.java, String::class.java, String::class.java), "bindings", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(proxyConstructorBuilder.build())
                .addMethod(resolveMethodBuilder.build())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(BindingDependencyResolver::class.java)
                .build()

        val proxyFile = JavaFile.builder(PACKAGE_NAME, proxySpec)
                .build()

        proxyFile.writeTo(filer)

        return true
    }

    private fun resolveBindings(element: Element): List<BindingTo> {
        val bindings = mutableListOf<BindingTo>()

        val annotationMirrors = element.annotationMirrors.filter { mirror ->
            mirror.annotationType.toString() == BindsTo::class.qualifiedName
        }

        for (annotationMirror in annotationMirrors) {
            var dependency: TypeMirror? = null
            var module: TypeMirror? = null

            for ((key, value) in annotationMirror.elementValues) {
                if (key.simpleName.toString() == BINDS_TO_PROPERTY_DEPENDENCY) {
                    dependency = value.value as TypeMirror
                } else if (key.simpleName.toString() == BINDS_TO_PROPERTY_MODULE) {
                    module = value.value as TypeMirror
                }
            }

            if (dependency != null && module != null) {
                bindings.add(BindingTo(element.asType().toString(), dependency.toString(), dependency, module.toString(), module))
            } else {
                throw IllegalArgumentException("Found $element with an incorrect configuration. Dependency and Module are required but are $dependency and $module")
            }
        }

        return bindings
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(BindsTo::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
}