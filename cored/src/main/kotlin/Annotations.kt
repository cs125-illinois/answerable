@file:Suppress("MatchingDeclarationName")

package edu.illinois.cs.cs125.answerable.core

import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Edge {
    companion object {
        fun validate(field: Field): Class<*> {
            check(field.isStatic()) { "@Edge fields must be static" }
            check(field.type.isArray) { "@Edge fields must annotate arrays" }
            return field.type.componentType
        }
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Simple {
    companion object {
        fun validate(field: Field): Class<*> {
            check(field.isStatic()) { "@Simple fields must be static" }
            check(field.type.isArray) { "@Simple fields must annotate arrays" }
            return field.type.componentType
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