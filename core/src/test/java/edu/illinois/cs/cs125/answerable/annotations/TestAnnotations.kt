package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.KotlinMode
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.languageMode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal fun String.test(): Class<*> {
    return Class.forName("${TestAnnotations::class.java.packageName}.fixtures.$this")
}

class TestAnnotations {
    @Test
    fun `should validate @Solution correctly`() {
        listOf("solution.Correct0", "solution.Broken0", "solution.Broken1", "solution.Broken2").map { it.test() }
            .forEach { klass ->
                Solution.validate(klass.testingValidateContext()).also { errors ->
                    if (klass.name.contains("Correct")) {
                        assert(errors.isEmpty())
                    } else {
                        assert(errors.isNotEmpty())
                        assert(errors.all { it.kind == AnnotationError.Kind.Solution })
                    }
                }
            }
        findAnnotation(Solution::class.java, "examples").also { klasses ->
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Solution.validate(klass.testingValidateContext()).isEmpty() })
        }
    }

    @Test
    fun `should validate @Precondition correctly`() {
        "TestValidatePrecondition".test().also { klass ->
            assert(klass.declaredMethods.hasAnyAnnotation(Precondition::class.java).size == 6)
            Precondition.validate(klass.testingValidateContext()).also { errors ->
                assert(errors.size == 3)
                assert(errors.all { it.kind == AnnotationError.Kind.Precondition })
                assert(
                    errors.all {
                        it.location.methodName?.contains("broken") ?: false
                    }
                )
            }
        }
        "TestDuplicatePrecondition".test().also { klass ->
            assert(klass.declaredMethods.hasAnyAnnotation(Precondition::class.java).size == 2)
            Precondition.validate(klass.testingValidateContext()).also { errors ->
                assert(errors.size == 1)
                assert(errors.all { it.kind == AnnotationError.Kind.Precondition })
            }
        }
        findAnnotation(Precondition::class.java, "examples").also { klasses ->
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Precondition.validate(klass.testingValidateContext()).isEmpty() })
        }
    }

    @Test
    @Disabled
    fun `should validate @Precondition correctly in Kotlin`() {
        "TestValidatePreconditionKt".test().also { klass ->
            assert(
                KotlinMode.findControlClass(klass, TypePool())!!
                    .declaredMethods.hasAnyAnnotation(Precondition::class.java).size == 2
            )
            Precondition.validate(klass.testingValidateContext()).also { errors ->
                assert(errors.size == 2)
                assert(errors.all { it.kind == AnnotationError.Kind.Precondition })
                assert(
                    errors.all {
                        it.location.methodName?.contains("broken") ?: false
                    }
                )
            }
        }
    }

    @Test
    fun `should validate @Verify correctly`() {
        "TestValidateVerify".test().also { klass ->
            assert(klass.declaredMethods.hasAnyAnnotation(Verify::class.java).size == 3)
            Verify.validate(klass.testingValidateContext()).also { errors ->
                assert(errors.size == 1) { errors }
                assert(errors.all { it.kind == AnnotationError.Kind.Verify })
                assert(
                    errors.all {
                        it.location.methodName?.contains("broken") ?: false
                    }
                )
            }
        }
        "TestDuplicateVerify".test().also { klass ->
            assert(klass.declaredMethods.hasAnyAnnotation(Verify::class.java).size == 2)
            Verify.validate(klass.testingValidateContext()).also { errors ->
                assert(errors.size == 1)
                assert(errors.all { it.kind == AnnotationError.Kind.Verify })
            }
        }
        findAnnotation(Verify::class.java, "examples").also { klasses ->
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Verify.validate(klass.testingValidateContext()).isEmpty() })
        }
    }

    @Test
    fun `should validate @Verify correctly in Kotlin`() {
        "TestValidateVerifyKt".test().testingValidateContext().also { context ->
            assert(context.controlClass.declaredMethods.hasAnyAnnotation(Verify::class.java).size == 4)
            Verify.validate(context).also { errors ->
                assert(errors.size == 2) { errors }
                assert(errors.all { it.kind == AnnotationError.Kind.Verify })
                assert(
                    errors.all {
                        it.location.methodName?.contains("broken") ?: false
                    }
                )
            }
        }
    }

    @Test
    fun `should validate @Timeout correctly`() {
        val klass = "TestValidateTimeout".test()
        assert(klass.declaredMethods.size == 7)
        Timeout.validate(klass.testingValidateContext()).also { errors ->
            assert(errors.size == 5)
            assert(errors.all { it.kind == AnnotationError.Kind.Timeout })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
        findAnnotation(Timeout::class.java, "examples").also { klasses ->
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Timeout.validate(klass.testingValidateContext()).isEmpty() })
        }
    }

    @Test
    @Disabled
    fun `should validate @Next correctly`() {
        val klass = "TestValidateNext".test()
        assert(klass.declaredMethods.size == 8)
        Next.oldValidate(klass).also { errors ->
            assert(errors.size == 7)
            assert(errors.all { it.kind == AnnotationError.Kind.Next })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }

        findAnnotation(Next::class.java, "examples").also { klasses ->
            klasses.forEach {
                val errors = Next.oldValidate(it)
                if (errors.isNotEmpty()) {
                    println(errors)
                }
            }
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Next.oldValidate(klass).isEmpty() })
        }
    }

    @Test
    @Disabled
    fun `should validate @Next correctly in Kotlin`() {
        val klassKt = "TestValidateNextKt".test()
        Next.oldValidate(klassKt).also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == AnnotationError.Kind.Next })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }

    @Test
    fun `should validate @Generator correctly`() {
        val klass = "TestValidateGenerator".test()
        assert(klass.declaredMethods.size == 4)
        Generator.validate(klass.testingValidateContext()).also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == AnnotationError.Kind.Generator })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
        findAnnotation(Generator::class.java, "examples").also { klasses ->
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Generator.validate(klass.testingValidateContext()).isEmpty() })
        }
    }

    @Test
    fun `should validate @Generator correctly in Kotlin`() {
        val klass = "TestValidateGeneratorKt".test()
        assert(klass.declaredMethods.isEmpty())
        Generator.validate(klass.testingValidateContext()).also { errors ->
            assert(errors.size == 2)
            assert(errors.all { it.kind == AnnotationError.Kind.Generator })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }

    @Test
    fun `should validate @EdgeCase correctly`() {
        val klass = "TestValidateEdgeCase".test()
        assert(klass.declaredMethods.size + klass.declaredFields.size == 14)
        EdgeCase.validate(klass).also { errors ->
            assert(errors.size == 10) { errors.size }
            assert(errors.all { it.kind == AnnotationError.Kind.EdgeCase })
            assert(
                errors.all {
                    it.location.methodName?.contains("broken") ?: it.location.fieldName?.contains("broken") ?: false
                }
            )
        }
    }

    @Test
    fun `should validate @SimpleCase correctly`() {
        val klass = "TestValidateSimpleCase".test()
        assert(klass.declaredMethods.size + klass.declaredFields.size == 14)
        SimpleCase.validate(klass).also { errors ->
            assert(errors.size == 10)
            assert(errors.all { it.kind == AnnotationError.Kind.SimpleCase })
            assert(
                errors.all {
                    it.location.methodName?.contains("broken") ?: it.location.fieldName?.contains("broken") ?: false
                }
            )
        }
    }

    @Test
    fun `should validate @DefaultRunArguments correctly`() {
        val klass = "TestValidateDefaultTestRunArguments".test()
        assert(klass.declaredMethods.size == 4)
        DefaultTestRunArguments.validate(klass.testingValidateContext()).also { errors ->
            assert(errors.size == 2) { errors }
            assert(errors.all { it.kind == AnnotationError.Kind.DefaultTestRunArguments })
            assert(errors.all { it.location.methodName?.contains("broken") ?: false })
        }
    }
}

private fun Class<*>.testingValidateContext() = ValidateContext(
    this,
    this.languageMode().findControlClass(this, TypePool()) ?: this
)