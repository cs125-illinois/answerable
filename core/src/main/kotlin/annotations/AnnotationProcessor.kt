package edu.illinois.cs.cs125.answerable.annotations

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeMirror

@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedOptions()
@SupportedAnnotationTypes("edu.illinois.cs.cs125.answerable.annotations.Solution")
class AnswerableInterfaceProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (annotation: TypeElement in annotations) {
            if (!annotation.simpleName.contentEquals("Solution")) continue

            val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)
            val annotatedMethods = annotatedElements.filter { it.kind == ElementKind.METHOD }
            val classes = annotatedMethods.groupBy { it.enclosingElement }.map { it.key }
            classes.forEach { processClass(it as TypeElement, roundEnv) }
        }

        return true
    }

    private fun makeInterfaceName(clazz: TypeElement): String {
        val className = clazz.simpleName
        return "I$className"
    }

    private fun processClass(clazz: TypeElement, roundEnv: RoundEnvironment) {
        if (clazz.getAnnotation(Metadata::class.java) == null) {
            processJavaClass(clazz, roundEnv)
        } else {
            // processKotlinClass(clazz, roundEnv)
        }
    }

    private fun processJavaClass(clazz: TypeElement, roundEnv: RoundEnvironment) {
        val packageName = processingEnv.elementUtils.getPackageOf(clazz).qualifiedName.toString()
        val interfaceName = makeInterfaceName(clazz)
        val interfaceMethods: List<ExecutableElement> =
            clazz.enclosedElements.filter { element ->
                element.kind == ElementKind.METHOD && methodShouldBeUsed(element)
            }.map { it as ExecutableElement }
        val implementedInterfaces = clazz.interfaces

        // Construct interface and write to file
        val iface = makeJavaInterface(
            packageName,
            TypeName.get(clazz.asType()),
            ClassName.get(packageName, interfaceName),
            implementedInterfaces,
            interfaceMethods
        ).addOriginatingElement(clazz)
            .build()

        val file = JavaFile.builder(packageName, iface).build()
        file.writeTo(processingEnv.filer)
    }

    private fun makeJavaInterface(
        packageName: String,
        className: TypeName,
        interfaceName: TypeName,
        implementedInterfaces: List<TypeMirror>,
        interfaceMethods: List<ExecutableElement>
    ): TypeSpec.Builder {
        fun fixTypeName(name: TypeName): TypeName = fixTypeName(className, interfaceName, name)
        fun makeJavaMethodSpec(method: ExecutableElement): MethodSpec {
            val methodType = method.asType() as ExecutableType
            val builder = MethodSpec.methodBuilder(method.simpleName.toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(fixTypeName(TypeName.get(methodType.returnType)))

            val params = methodType.parameterTypes.zip(method.parameters)
            for (param in params) {
                builder.addParameter(
                    fixTypeName(TypeName.get(param.first)),
                    param.second.simpleName.toString()
                )
            }

            // TODO: enahance :)
            return builder.build()
        }

        return TypeSpec.interfaceBuilder((interfaceName as ClassName).simpleName())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterfaces(implementedInterfaces.map { fixTypeName(TypeName.get(it)) })
            .addMethods(interfaceMethods.map(::makeJavaMethodSpec))
    }

    private val annotationsThatIgnore = controlAnnotations
    @Suppress("ReturnCount")
    private fun methodShouldBeUsed(element: Element): Boolean {
        if (Modifier.PUBLIC !in element.modifiers) {
            return false
        }
        annotationsThatIgnore.forEach { annotation ->
            if (element.getAnnotation(annotation) != null) {
                return false
            }
        }
        return true
    }
}

private fun fixTypeName(
    className: TypeName,
    interfaceName: TypeName,
    typeName: TypeName
): TypeName = when (typeName) {
    is ClassName -> if (typeName == className) interfaceName else typeName
    is ArrayTypeName -> if (typeName.componentType == className) ArrayTypeName.of(interfaceName) else typeName
    is ParameterizedTypeName -> ParameterizedTypeName.get(
        fixTypeName(className, interfaceName, typeName.rawType) as ClassName,
        *typeName.typeArguments.map { fixTypeName(className, interfaceName, it) }.toTypedArray()
    )
    is TypeVariableName -> TypeVariableName.get(
        typeName.name,
        *typeName.bounds.map { fixTypeName(className, interfaceName, it) }.toTypedArray()
    )
    // JavaPoet doesn't support multiple bounds, which means generated interfaces won't either.
    is WildcardTypeName -> when {
        typeName.upperBounds.isNotEmpty() ->
            WildcardTypeName.subtypeOf(fixTypeName(className, interfaceName, typeName.upperBounds[0]))
        typeName.lowerBounds.isNotEmpty() ->
            WildcardTypeName.supertypeOf(fixTypeName(className, interfaceName, typeName.lowerBounds[0]))
        else ->
            typeName
    }
    else -> typeName
}
