package edu.illinois.cs.cs125.answerable

import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun Method.isStatic() = Modifier.isStatic(modifiers)
