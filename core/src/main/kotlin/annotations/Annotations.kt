@file:Suppress("UNUSED", "TooManyFunctions", "MatchingDeclarationName", "SpreadOperator")

package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import io.github.classgraph.ClassGraph
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class AnnotationError(val kind: Kind, val location: SourceLocation, val message: String) {
    enum class Kind {
        Solution, Precondition, Verify, Generator, Next, EdgeCase, SimpleCase, Timeout, DefaultTestRunArguments
    }
}

internal fun Class<*>.validateAnnotations(typePool: TypePool = TypePool()) {
    val annotationErrors = mutableListOf<AnnotationError>()

    annotationErrors.addAll(Solution.validate(this))
    annotationErrors.addAll(Precondition.validate(this))
    annotationErrors.addAll(Verify.validate((this)))
    annotationErrors.addAll(Timeout.validate(this))
    annotationErrors.addAll(Next.validate(this))
    annotationErrors.addAll(Generator.validate(this))
    annotationErrors.addAll(EdgeCase.validate(this))
    annotationErrors.addAll(SimpleCase.validate(this))
    annotationErrors.addAll(DefaultTestRunArguments.validate(this))

    if (annotationErrors.isEmpty()) {
        return
    } else {
        throw AnswerableMisuseException(
            annotationErrors.joinToString(separator = ",") { "${it.location}: ${it.message}" }
        )
    }
}

data class ValidateContext(val referenceClass: Class<*>, val controlClass: Class<*>)

internal fun Method.validateCase(): String? {
    return if (!Modifier.isStatic(modifiers)) {
        "method must be static"
    } else if (!parameterTypes.isEmpty()) {
        "method must not accept any parameters"
    } else if (!returnType.isArray) {
        "method must return an array"
    } else {
        null
    }
}

internal fun Field.validateCase(): String? {
    return if (!Modifier.isStatic(modifiers)) {
        "field must be static"
    } else if (!type.isArray) {
        "field must store an array"
    } else {
        null
    }
}

internal fun Array<Method>.hasAnyAnnotation(klasses: Collection<Class<out Annotation>>): List<Method> =
    this.hasAnyAnnotation(*klasses.toTypedArray())

internal fun Array<Method>.hasAnyAnnotation(vararg klasses: Class<out Annotation>): List<Method> =
    this.filter { method -> klasses.any { method.isAnnotationPresent(it) } }

internal fun Method.areAnnotationsPresent(klasses: Collection<Class<out Annotation>>): Boolean =
    this.areAnnotationsPresent(*klasses.toTypedArray())

internal fun Method.areAnnotationsPresent(vararg klasses: Class<out Annotation>): Boolean =
    klasses.all { this.isAnnotationPresent(it) }

internal fun Class<*>.methodsWithAnyAnnotation(klasses: Collection<Class<out Annotation>>): List<Method> =
    this.methodsWithAnyAnnotation(*klasses.toTypedArray())

internal fun Class<*>.methodsWithAnyAnnotation(vararg klasses: Class<out Annotation>): List<Method> =
    this.declaredMethods.hasAnyAnnotation(*klasses)

internal fun findAnnotation(klass: Class<out Annotation>, prefix: String = "") = ClassGraph()
    .enableAllInfo()
    .acceptPackages(prefix)
    .scan()
    .allClasses
    .map {
        it.loadClass()
    }.filter {
        it.methodsWithAnyAnnotation(klass).isNotEmpty()
    }

internal fun Method.ifHasAnnotation(
    klass: Class<out Annotation>,
    check: (method: Method) -> AnnotationError?
): AnnotationError? {
    return if (isAnnotationPresent(klass)) {
        check(this)
    } else {
        null
    }
}

internal fun Field.ifHasAnnotation(
    klass: Class<out Annotation>,
    check: (field: Field) -> AnnotationError?
): AnnotationError? {
    return if (isAnnotationPresent(klass)) {
        check(this)
    } else {
        null
    }
}

// TODO:
// How I envision this looking from the perspective of an annotation is that it can call either
// `validateReferenceAnnotation` or `validateControlAnnotation`. But then they need access to a TypePool.
// Note that everything is a control annotation except @Solution, and that @DefaultTestRunArguments and @Timeout
// can be either or even both.
//
// I feel like the right way to do this is to make the validation methods take a context instead of just a reference
// class, and the context, created in `Class<*>.validateAnnotations` above, holds onto the reference class
// and the control class. This is also easier to extend in the future.
//
// Under this, `validateReferenceAnnotation` and `validateControlAnnotation` become methods of the context.

internal fun ValidateContext.validateReferenceAnnotation(
    annotationClass: Class<out Annotation>,
    methodValidator: ((method: Method) -> AnnotationError?)? = null,
    fieldValidator: ((field: Field) -> AnnotationError?)? = null
): List<AnnotationError> = this.referenceClass.validateAnnotation(annotationClass, methodValidator, fieldValidator)

/**
 * If the validator needs access to the reference class, it may caputure the ValidateContext in the validators.
 */
internal fun ValidateContext.validateControlAnnotation(
    annotationClass: Class<out Annotation>,
    methodValidator: ((method: Method) -> AnnotationError?)? = null,
    fieldValidator: ((field: Field) -> AnnotationError?)? = null
): List<AnnotationError> = this.controlClass.validateAnnotation(annotationClass, methodValidator, fieldValidator)

internal fun ValidateContext.validateAnnotation(
    annotationClass: Class<out Annotation>,
    methodValidator: ((method: Method) -> AnnotationError?)? = null,
    fieldValidator: ((field: Field) -> AnnotationError?)? = null
): List<AnnotationError> =
    this.validateReferenceAnnotation(annotationClass, methodValidator, fieldValidator) +
        this.validateControlAnnotation(annotationClass, methodValidator, fieldValidator)

internal fun Class<*>.validateAnnotation(
    annotationClass: Class<out Annotation>,
    methodValidator: ((method: Method) -> AnnotationError?)? = null,
    fieldValidator: ((field: Field) -> AnnotationError?)? = null
) = this.validateMembers(
    methodValidator = methodValidator?.let { mv ->
        { method: Method ->
            method.ifHasAnnotation(annotationClass, mv)
        }
    },
    fieldValidator = fieldValidator?.let { fv ->
        { field: Field ->
            field.ifHasAnnotation(annotationClass, fv)
        }
    }
)

internal fun Class<*>.validateMembers(
    methodValidator: ((method: Method) -> AnnotationError?)? = null,
    fieldValidator: ((field: Field) -> AnnotationError?)? = null
): List<AnnotationError> {
    return mutableListOf<AnnotationError>().also { list ->
        methodValidator?.also {
            list.addAll(declaredMethods.map { method -> it(method) }.filterNotNull())
        }
        fieldValidator?.also {
            list.addAll(declaredFields.map { field -> it(field) }.filterNotNull())
        }
    }
}

const val DEFAULT_EMPTY_NAME = "(empty)"

private val namedAnnotations = listOf(Solution::class.java, Precondition::class.java, Verify::class.java)

// TODO: `require` throws IllegalArgumentExceptions... do we want AnswerableMisuseExceptions? I think so.
// note that the last `require` is actually a panic condition and should be an IllegalStateException.
internal fun Executable.solutionName(): String {
    val namingAnnotation = namedAnnotations.filter { isAnnotationPresent(it) }.let {
        require(it.isNotEmpty()) { "Can't find naming annotation for $name" }
        require(it.size == 1) { "Multiple naming annotations for $name" }
        it.first()
    }

    val annotatedName: String? = when (namingAnnotation) {
        Solution::class.java -> getAnnotation(Solution::class.java).name
        Precondition::class.java -> getAnnotation(Precondition::class.java).name
        Verify::class.java -> getAnnotation(Verify::class.java).name
        else -> null
    }
    require(annotatedName != null) { "Invalid naming annotation ${namingAnnotation.name}" }
    return annotatedName
}

internal fun Class<*>.getNamedAnnotation(
    klass: Class<out Annotation>,
    solutionName: String = DEFAULT_EMPTY_NAME
): Method? {
    val methods = declaredMethods.hasAnyAnnotation(klass)
    if (methods.isEmpty()) {
        return null
    }

    return methods.filter { it.solutionName() == solutionName }.let { matchingMethods ->
        require(matchingMethods.size <= 1) { "Found multiple @${klass.name} methods name $solutionName" }
        matchingMethods.firstOrNull()?.also { it.isAccessible = true }
    }
}

internal fun List<Method>.duplicateSolutionNames() = this
    .map { method -> method.solutionName() }
    .groupingBy { it }
    .eachCount()
    .filter { (_, count) -> count > 1 }
    .values
