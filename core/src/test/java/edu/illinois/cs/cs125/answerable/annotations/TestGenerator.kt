package edu.illinois.cs.cs125.answerable.annotations

import org.junit.jupiter.api.Test

class TestGenerator {
    @Test
    fun `should validate @Generator correctly`() {
        listOf("generator.Correct0", "generator.Broken0").map { it.test() }
            .forEach { klass ->
                Generator.validate(klass).also { errors ->
                    if (klass.name.contains("Correct")) {
                        assert(errors.isEmpty())
                    } else {
                        assert(errors.isNotEmpty())
                        assert(errors.all { it.kind == AnnotationError.Kind.Generator })
                    }
                }
            }
        findAnnotation(Generator::class.java, "examples").also { klasses ->
            assert(klasses.isNotEmpty())
            assert(klasses.all { klass -> Solution.validate(klass).isEmpty() })
        }
    }

    @Test
    fun `should validate grouped @Generator correctly`() {
        "generator.Grouped".test().also { klass ->
            klass.getAllSolutions().also {
                assert(it.isNotEmpty())
            }.forEach { method ->
                val name = method.getAnnotation(Solution::class.java).name
                assert(klass.getGroupedGenerator(name) != null)
            }
        }
    }
}
