@file:Suppress("MatchingDeclarationName")

package edu.illinois.cs.cs125.answerable.core

import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Random

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Simple {
    companion object {
        fun validateAsType(field: Field): Class<*> {
            check(field.isStatic()) { "@Simple fields must be static" }
            check(field.type.isArray) { "@Simple fields must annotate arrays" }
            field.isAccessible = true
            return field.type.componentType
        }

        fun validateAsParameters(field: Field): Array<Type> {
            check(field.isStatic()) { "@Simple fields must be static" }
            check(field.genericType is ParameterizedType) {
                "@Simple parameter fields must annotate parameterized collections"
            }
            (field.genericType as ParameterizedType).also { collectionType ->
                check(collectionType.actualTypeArguments.size == 1) {
                    "@Simple parameter fields must annotate parameterized collections"
                }
                collectionType.actualTypeArguments.first().also { itemType ->
                    check(itemType is ParameterizedType && itemType.rawType in parameterGroupTypes) {
                        "@Simple parameter fields must annotate collections of types " +
                            parameterGroupTypes.joinToString(", ") { it.simpleName }
                    }
                    field.isAccessible = true
                    return itemType.actualTypeArguments
                }
            }
        }
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Edge {
    companion object {
        fun validateAsType(field: Field): Class<*> {
            check(field.isStatic()) { "@Edge fields must be static" }
            check(field.type.isArray) { "@Edge fields must annotate arrays" }
            field.isAccessible = true
            return field.type.componentType
        }
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Rand {
    companion object {
        fun validateAsType(method: Method): Class<*> {
            check(method.isStatic()) { "@Random methods must be static" }
            check(
                method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == Int::class.java &&
                    method.parameterTypes[1] == Random::class.java
            ) {
                "@Random methods must accept parameters (int complexity, Random random)"
            }
            method.isAccessible = true
            return method.returnType
        }
    }
}

fun Field.isStatic() = Modifier.isStatic(modifiers)
fun Any.asArray(): Array<*> {
    return when (this) {
        is ByteArray -> this.toTypedArray()
        is ShortArray -> this.toTypedArray()
        is IntArray -> this.toTypedArray()
        is LongArray -> this.toTypedArray()
        is FloatArray -> this.toTypedArray()
        is DoubleArray -> this.toTypedArray()
        is CharArray -> this.toTypedArray()
        is BooleanArray -> this.toTypedArray()
        else -> this as Array<*>
    }
}

fun Executable.isAnswerable() = isAnnotationPresent(Rand::class.java)
fun Field.isAnswerable() = isAnnotationPresent(Simple::class.java) || isAnnotationPresent(Edge::class.java)

data class One<I>(val first: I)
data class Two<I, J>(val first: I, val second: J)
data class Three<I, J, K>(val first: I, val second: J, val third: K)
data class Four<I, J, K, L>(val first: I, val second: J, val third: K, val fourth: L)

private val parameterGroupTypes = setOf(One::class.java, Two::class.java, Three::class.java, Four::class.java)

fun Field.isEdgeOrSimpleType() = if (isAnnotationPresent(Simple::class.java) || isAnnotationPresent(Edge::class.java)) {
    type.isArray
} else {
    false
}

fun Field.isEdgeOrSimpleParameters() =
    if (isAnnotationPresent(Simple::class.java) || isAnnotationPresent(Edge::class.java)) {
        genericType is ParameterizedType &&
            ((genericType as ParameterizedType).actualTypeArguments.first() is ParameterizedType) &&
            (((genericType as ParameterizedType).actualTypeArguments.first() as ParameterizedType).rawType
                in parameterGroupTypes)
    } else {
        false
    }