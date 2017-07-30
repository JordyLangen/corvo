package com.jordylangen.corvo.compiler

import com.jordylangen.corvo.ComponentProxy
import com.jordylangen.corvo.annotations.BindsTo
import com.squareup.javapoet.*
import dagger.Component
import java.lang.IllegalArgumentException
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types


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
        private const val CORVO_COMPONENT = "CorvoComponent"
        private const val CORVO_COMPONENT_ARGUMENT = "component"
        private const val RESOLVE_METHOD = "resolve"
        private const val CLASS_EXTENSION = ".class"
        private const val CORVO_COMPONENT_PROXY = "CorvoComponentProxy"
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

        val dependencies = bindings.map { binding -> binding.dependency }.distinct()
        val modules = bindings.map { binding -> binding.module }.distinct()

        createComponent(dependencies, modules)
        createComponentProxy(bindings)

        return true
    }

    private fun resolveBindings(element: Element): List<BindingTo> {
        val bindings = mutableListOf<BindingTo>()

        val annotationMirrors = element.annotationMirrors.filter { mirror ->
            mirror.annotationType.toString() == BindsTo::class.qualifiedName
        }

        for (annotationMirror in annotationMirrors) {
            var dependency: TypeElement? = null
            var module: TypeElement? = null

            for ((key, value) in annotationMirror.elementValues) {
                if (key.simpleName.toString() == BINDS_TO_PROPERTY_DEPENDENCY) {
                    dependency = typeUtils.asElement(value.value as TypeMirror) as TypeElement
                } else if (key.simpleName.toString() == BINDS_TO_PROPERTY_MODULE) {
                    module = typeUtils.asElement(value.value as TypeMirror) as TypeElement
                }
            }

            if (dependency != null && module != null) {
                bindings.add(BindingTo(element as TypeElement, dependency, module))
            } else {
                throw IllegalArgumentException("Found $element with an incorrect configuration. Dependency and Module are required but are $dependency and $module")
            }
        }

        return bindings
    }

    private fun createComponent(dependencies: List<TypeElement>, modules: List<TypeElement>) {
        val componentAnnotation = AnnotationSpec.builder(Component::class.java)
                .addMember(DAGGER_PROPERTY_MODULES, modules.map { "${it.qualifiedName}$CLASS_EXTENSION" }.joinToString(separator = ", ", prefix = "{ ", postfix = " }"))
                .build()

        val componentMethods = dependencies.map { dependency ->
            MethodSpec.methodBuilder("$RESOLVE_METHOD${dependency.simpleName}")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeName.get(dependency.asType()))
                    .build()
        }

        val corvoComponentSpec = TypeSpec.interfaceBuilder(CORVO_COMPONENT)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation)
                .addMethods(componentMethods)
                .build()

        val componentFile = JavaFile.builder(PACKAGE_NAME, corvoComponentSpec)
                .build()

        componentFile.writeTo(filer)
    }

    private fun createComponentProxy(bindings: List<BindingTo>) {
        val proxyConstructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(PACKAGE_NAME, CORVO_COMPONENT), CORVO_COMPONENT_ARGUMENT)
                .addStatement("this.component = component")
                .addStatement("this.bindings = new HashMap<String, String>()")

        for ((dependent, dependency) in bindings) {
            proxyConstructorBuilder.addStatement("this.bindings.put(\"${dependent.qualifiedName}\", \"${dependency.qualifiedName}\")")
        }

        val resolveMethodBuilder = MethodSpec.methodBuilder(RESOLVE_METHOD)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .addParameter(String::class.java, "className")
                .addStatement("String dependency = bindings.get(className)")
                .addStatement("if (!bindings.containsKey(dependency)) { return null; }")
                .returns(TypeVariableName.get("T"))

        for (binding in bindings) {
            resolveMethodBuilder.addStatement("if (dependency.equals(\"${binding.dependency.qualifiedName}\")) { return (T) component.resolve${binding.dependency.simpleName}(); }")
        }

        resolveMethodBuilder.addStatement("return null")

        val proxySpec = TypeSpec.classBuilder(CORVO_COMPONENT_PROXY)
                .addField(ClassName.get(PACKAGE_NAME, CORVO_COMPONENT), CORVO_COMPONENT_ARGUMENT, Modifier.PRIVATE, Modifier.FINAL)
                .addField(ParameterizedTypeName.get(HashMap::class.java, String::class.java, String::class.java), "bindings", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(proxyConstructorBuilder.build())
                .addMethod(resolveMethodBuilder.build())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ComponentProxy::class.java)
                .build()

        val proxyFile = JavaFile.builder(PACKAGE_NAME, proxySpec)
                .build()

        proxyFile.writeTo(filer)
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(BindsTo::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
}