package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.isStatic
import edu.illinois.cs.cs125.answerable.sourceName
import java.lang.reflect.Method
import kotlin.Pair as KPair

/**
 * Marks a method which can produce objects (or primitives) of arbitrary type for testing.
 *
 * The method must be static and take 2 parameters;
 * (1) an <tt>int</tt> representing the maximum complexity level of the output that should be produced, and
 * (2) a [java.util.Random] instance to be used if randomness is required.
 * The visibility and name do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an [AnswerableMisuseException] will be thrown.
 *
 * Answerable will automatically detect the return type and override any existing generators for that type.
 * If the generator generates instance of the reference class, Answerable will automatically manage the transformation
 * required to use the method to generate instances of the submitted class. Due to this behavior, methods marked with
 * @[Generator] and whose return type is of the reference class <b>must</b> only use the <tt>public</tt> features
 * of the reference class, specifically those which the class design analysis pass will see. Answerable tries to
 * verify that your generators are safe and raise [AnswerableVerificationException]s if there is a problem.
 *
 * If a generator for the reference class is provided, and an @[Next] method is not provided, then the generator
 * will be used to generate new receiver objects on every iteration of the testing loop.
 *
 * @[Generator] annotations can have a [name] parameter, which must be unique.
 * If your class provides multiple generators for the same type, Answerable will resolve conflicts by choosing the one
 * whose [name] is in the [Solution.enabled] array on the @[Solution] annotation.
 *
 * If a helper method is needed that should not be included in class design analysis, see the @[Helper] annotation.
 * Generators can safely call each other and the @[Next] method (if any), even those which create instances
 * of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Generator(
    val name: String = ""
) {
    companion object {
        private val parameterTypes = arrayOf(Int::class.java, java.util.Random::class.java)

        fun validate(context: ValidateContext) = context.validateControlAnnotation(
            Generator::class.java,
            ::validateMethod
        )

        private fun validateMethod(method: Method): AnnotationError? {
            val message = if (!method.isStatic()) {
                "@Generator methods must be static"
            } else if (!(method.parameterTypes contentEquals parameterTypes)) {
                "@Generator methods must take parameters (int, Random)"
            } else {
                null
            }
            return if (message != null) {
                AnnotationError(
                    AnnotationError.Kind.Generator,
                    SourceLocation(method),
                    message
                )
            } else {
                null
            }
        }
    }
}

internal val Generator.usableName: String?
    get() = if (this.name == "") {
        null
    } else {
        this.name
    }

/**
 * Marks that a method parameter should use a particular generator.
 *
 * The selected generator does not need to be explicitly enabled under the @[Solution] annotation.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class UseGenerator(
    val name: String
)

internal fun Method.isGenerator() = isAnnotationPresent(Generator::class.java)
// TODO: this is always true, if the method is a generator, because Generator.name cannot be null
// This function is most likely no longer necessary following the refactor.
internal fun Method.isNamedGenerator() =
    isAnnotationPresent(Generator::class.java) && getAnnotation(Generator::class.java)?.name != null ?: false

internal fun Class<*>.getAllGenerators(): List<Method> =
    declaredMethods.filter { it.isGenerator() }.map { it.isAccessible = true; it }

internal fun Class<*>.getAllNamedGenerators(): List<KPair<String, Method>> =
    declaredMethods.filter { it.isNamedGenerator() }.map { it.isAccessible = true; it }.map {
        KPair(it.getAnnotation(Generator::class.java)!!.name, it)
    }

internal fun Class<*>.getNamedGenerators(name: String = ""): List<Method> =
    declaredMethods.filter { it.isGenerator() }.filter {
        (
            it.getAnnotation(Generator::class.java)?.name?.let {
                if (it.isEmpty()) {
                    DEFAULT_EMPTY_NAME
                } else {
                    it
                }
            }
            ) ?: DEFAULT_EMPTY_NAME == name
    }

internal fun Class<*>.getEnabledGenerators(enabledNames: Array<String>): List<Method> =
    declaredMethods
        .filter { it.isGenerator() }
        .groupBy { it.genericReturnType }
        .flatMap { entry ->
            when (entry.value.size) {
                1 -> entry.value
                else -> {
                    entry.value
                        .filter { it.getAnnotation(Generator::class.java).name in enabledNames }
                        .let { enabledGenerators ->
                            when (enabledGenerators.size) {
                                1 -> enabledGenerators
                                else -> {
                                    val name = entry.key.sourceName
                                    throw AnswerableMisuseException(
                                        "Failed to resolve @Generator conflict:\n" +
                                            "Multiple enabled generators found for type `$name'."
                                    )
                                }
                            }
                        }
                }
            }
        }
        .map { it.isAccessible = true; it }

data class Pair<I, J>(val first: I, val second: J)
data class Triple<I, J, K>(val first: I, val second: J, val third: K)
data class Quad<I, J, K, L>(val first: I, val second: J, val third: K, val fourth: L)

internal fun Class<*>.getGroupedGenerator(solutionName: String = ""): Method? {
    val solution = getSolution(solutionName) ?: return null
    return solution.matchGroupedReturn(getNamedGenerators(solutionName))
}

@Suppress("ReturnCount")
internal fun Method.matchGroupedReturn(candidates: Collection<Method>): Method? {
    val parameterTypes = parameterTypes.map { it.typeName }
    @Suppress("MagicNumber")
    val correctReturnType = when (parameterTypes.size) {
        2 -> Pair::class.java.name
        3 -> Triple::class.java.name
        4 -> Quad::class.java.name
        else -> return null
    }
    val returnTypeMatches = candidates.find { it.returnType.name == correctReturnType } ?: return null
    val returnTypes = returnTypeMatches.genericReturnType.typeName?.let {
        it.substring(it.indexOf("<") + 1, it.lastIndexOf(">"))
    }?.split(",")?.map { it.trim() } ?: return null

    return if (parameterTypes.map { it.normalizeType() } == returnTypes.map { it.normalizeType() }) {
        returnTypeMatches
    } else {
        null
    }
}

private val arrayEndRegex =
    """(.*?)(\[])*$""".toRegex()
private fun String.normalizeType(): String {
    val splitArray = arrayEndRegex.matchEntire(this) ?: error("Type comparison regex didn't match")
    val boxedType = when (val baseType = splitArray.groups[1]?.value ?: error("Type comparison regex didn't match")) {
        "boolean" -> "java.lang.Boolean"
        "byte" -> "java.lang.Byte"
        "char" -> "java.lang.Character"
        "float" -> "java.lang.Float"
        "int" -> "java.lang.Integer"
        "long" -> "java.lang.Long"
        "short" -> "java.lang.Short"
        "double" -> "java.lang.Double"
        else -> baseType
    }
    return if (splitArray.groups[2] != null) {
        "$boxedType${splitArray.groups[2]?.value ?: error("Type comparison regex didn't match")}"
    } else {
        boxedType
    }
}