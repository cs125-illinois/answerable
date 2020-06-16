package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.classdesignanalysis.answerableName
import edu.illinois.cs.cs125.answerable.classdesignanalysis.simpleName
import java.lang.reflect.Field
import java.lang.reflect.Method

data class SourceLocation(
    val packageName: String,
    val className: String? = null,
    val methodName: String? = null,
    val fieldName: String? = null
) {
    constructor(klass: Class<*>) : this(klass.packageName, klass.simpleName)
    constructor(method: Method) : this(
        method.declaringClass.packageName,
        method.declaringClass.simpleName,
        methodName = method.answerableName()
    )
    constructor(field: Field) : this(
        field.declaringClass.packageName,
        field.declaringClass.simpleName,
        fieldName = field.simpleName()
    )
}
