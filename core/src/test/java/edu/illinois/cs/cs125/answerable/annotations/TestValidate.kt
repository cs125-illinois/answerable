package edu.illinois.cs.cs125.answerable.annotations

import org.junit.jupiter.api.Test

private fun String.test(): Class<*> {
    return Class.forName("${TestValidate::class.java.packageName}.fixtures.$this")
}

class TestValidate {
    @Test
    fun `should validate @Next correctly`() {
        val klass = "TestValidateNext".test()
        assert(klass.declaredMethods.size == 4)
        klass.validateNext().also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == AnnotationUseError.Kind.Next })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }

    @Test
    fun `should validate @Generator correctly`() {
        val klass = "TestValidateGenerator".test()
        assert(klass.declaredMethods.size == 4)
        klass.validateGenerator().also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == AnnotationUseError.Kind.Generator })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }

    @Test
    fun `should validate @EdgeCase correctly`() {
        val klass = "TestValidateEdgeCase".test()
        assert(klass.declaredMethods.size + klass.declaredFields.size == 14)
        klass.validateEdgeCase().also { errors ->
            assert(errors.size == 10) { errors.size }
            assert(errors.all { it.kind == AnnotationUseError.Kind.EdgeCase })
            assert(errors.all {
                it.location.methodName?.contains("broken") ?: it.location.fieldName?.contains("broken") ?: false
            })
        }
    }

    @Test
    fun `should validate @SimpleCase correctly`() {
        val klass = "TestValidateSimpleCase".test()
        assert(klass.declaredMethods.size + klass.declaredFields.size == 14)
        klass.validateSimpleCase().also { errors ->
            assert(errors.size == 10) { errors.size }
            assert(errors.all { it.kind == AnnotationUseError.Kind.SimpleCase })
            assert(errors.all {
                it.location.methodName?.contains("broken") ?: it.location.fieldName?.contains("broken") ?: false
            })
        }
    }

    @Test
    fun `should validate @Precondition correctly`() {
        val klass = "TestValidatePrecondition".test()
        assert(klass.declaredMethods.hasAnyAnnotation(Precondition::class.java).size == 6)
        klass.validatePrecondition().also { errors ->
            assert(errors.size == 3) { errors.size }
            assert(errors.all { it.kind == AnnotationUseError.Kind.Precondition })
            assert(errors.all {
                it.location.methodName?.contains("broken") ?: false
            })
        }
    }

    @Test
    fun `should validate @DefaultRunArguments correctly`() {
        val klass = "TestValidateDefaultTestRunArguments".test()
        assert(klass.declaredMethods.size == 4)
        klass.validateDefaultTestRunArguments().also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == ValidationError.Kind.DefaultTestRunArguments })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }
}
