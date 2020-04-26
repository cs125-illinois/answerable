package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.classdesignanalysis.ValidationError
import edu.illinois.cs.cs125.answerable.classdesignanalysis.validateCaseMethods
import edu.illinois.cs.cs125.answerable.classdesignanalysis.validateDefaultTestRunArguments
import org.junit.jupiter.api.Test

private fun String.test(): Class<*> {
    return Class.forName("${TestValidate::class.java.packageName}.fixtures.$this")
}

class TestValidate {
    @Test
    fun `should test case methods correctly`() {
        val klass = "TestValidateCaseMethods".test()
        assert(klass.declaredMethods.size == 11)
        klass.validateCaseMethods().also { errors ->
            assert(errors.size == 7)
            assert(errors.all { it.kind == ValidationError.Kind.CaseMethods })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }
    @Test
    fun `should test default run arguments correctly`() {
        val klass = "TestValidateDefaultTestRunArguments".test()
        assert(klass.declaredMethods.size == 4)
        klass.validateDefaultTestRunArguments().also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == ValidationError.Kind.DefaultTestRunArguments })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }
}
