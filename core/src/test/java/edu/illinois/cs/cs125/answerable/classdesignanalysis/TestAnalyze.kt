package edu.illinois.cs.cs125.answerable.classdesignanalysis

import com.marcinmoskala.math.combinations
import org.junit.jupiter.api.Test

private fun String.example(): Class<*> {
    return Class.forName("${TestAnalyze::class.java.packageName}.fixtures.$this")
}

private fun Set<String>.correctPairs(type: String): Set<List<Class<*>>> =
    mutableSetOf<List<Class<*>>>().also { set ->
        this.forEach { set.add(listOf("$type.$it".example(), "$type.reference.$it".example())) }
        this.combinations(2).map { it.toList() }.forEach { (first, second) ->
            if (first.split("_").first() == second.split("_").first()) {
                set.add(listOf("$type.$first".example(), "$type.reference.$second".example()))
                set.add(listOf("$type.$second".example(), "$type.reference.$first".example()))
            }
        }
    }.toSet()

private fun Set<String>.incorrectPairs(type: String): Set<List<Class<*>>> =
    mutableSetOf<List<Class<*>>>().also { set ->
        this.combinations(2).map { it.toList() }.forEach { (first, second) ->
            if (first.split("_").first() != second.split("_").first()) {
                set.add(listOf("$type.$first".example(), "$type.reference.$second".example()))
                set.add(listOf("$type.$second".example(), "$type.reference.$first".example()))
            }
        }
    }.toSet()

class TestAnalyze {
    @Test
    fun `should check class names correctly`() {
        val klasses = setOf("Foo", "Bar")
        klasses.correctPairs("names").forEach { (first, second) ->
            first.namesMatch(second).also {
                assert(it.matched)
            }
        }
        klasses.incorrectPairs("names").forEach { (first, second) ->
            first.namesMatch(second).also {
                assert(!it.matched)
            }
        }
    }

    @Test
    fun `should check class types correctly`() {
        val klasses = setOf("Klass", "Interfase")
        klasses.correctPairs("types").forEach { (first, second) ->
            first.typesMatch(second).also {
                assert(it.matched)
            }
        }
        klasses.incorrectPairs("types").forEach { (first, second) ->
            first.typesMatch(second).also {
                assert(!it.matched)
            }
        }
    }

    @Test
    fun `should check class modifiers correctly`() {
        val klasses = setOf("Abstrict", "Finol", "StractFp")
        klasses.correctPairs("modifiers").forEach { (first, second) ->
            first.modifiersMatch(second).also {
                assert(it.matched)
            }
        }
        klasses.incorrectPairs("modifiers").forEach { (first, second) ->
            first.modifiersMatch(second).also {
                assert(!it.matched)
            }
        }
    }

    @Test
    fun `should check class parents correctly`() {
        val klasses = setOf("Parent", "Child", "Parent_ExtendsObject")
        klasses.correctPairs("parents").forEach { (first, second) ->
            first.parentsMatch(second).also {
                assert(it.matched)
            }
        }
        klasses.incorrectPairs("parents").forEach { (first, second) ->
            first.parentsMatch(second).also {
                assert(!it.matched)
            }
        }
    }

    @Test
    fun `should check class interfaces correctly`() {
        val klasses = setOf(
            "None",
            "ImplementsFoo",
            "ImplementsBar",
            "ImplementsFooBar",
            "ImplementsFoo_Again",
            "ImplementsBar_Again",
            "ImplementsFooBar_Again"
        )
        klasses.correctPairs("interfaces").forEach { (first, second) ->
            first.interfacesMatch(second).also {
                assert(it.matched)
            }
        }
        klasses.incorrectPairs("interfaces").forEach { (first, second) ->
            first.interfacesMatch(second).also {
                assert(!it.matched)
            }
        }
    }

    @Test
    fun `should check class fields correctly`() {
        "examples.classdesign.publicapi.fields.reference.Simple".load().publicFieldsMatch(
            "examples.classdesign.publicapi.fields.Simple".load()
        ).also {
            assert(it.matched)
        }
        "examples.classdesign.publicapi.fields.reference.Simple".load().publicFieldsMatch(
            "examples.classdesign.publicapi.fields.String".load()
        ).also {
            assert(!it.matched)
        }
    }

    @Test
    fun `should generate method names correctly`() {
        "examples.classdesign.publicapi.methods.Constructor".load().constructors.first()
            .answerableName().also {
                assert("public Constructor(int, boolean)" == it)
            }
        "examples.classdesign.publicapi.methods.MissingFinal".load().declaredMethods.first()
            .answerableName().also {
                assert("public <T> T foo(String, T)" == it)
            }
        "examples.classdesign.publicapi.methods.MissingThrow".load().declaredMethods.first()
            .answerableName().also {
                assert("public void foo() throws IllegalStateException" == it)
            }
        "examples.classdesign.publicapi.methods.Simple".load().declaredMethods.also { methods ->
            methods.find { it.name == "zero" }?.answerableName().also {
                assert("public static int zero()" == it)
            } ?: assert(false) { "Didn't find method" }
            methods.find { it.name == "toString" }?.answerableName().also {
                assert("public String toString()" == it)
            } ?: assert(false) { "Didn't find method" }
            methods.find { it.name == "lt" }?.answerableName().also {
                assert("public boolean lt(int, int)" == it)
            } ?: assert(false) { "Didn't find method" }
        }
        "examples.classdesign.publicapi.methods.Throws".load().declaredMethods.first()
            .answerableName().also {
                assert("public void foo() throws IllegalStateException, StackOverflowError" == it)
            }
        "examples.classdesign.publicapi.methods.TypeParam".load().declaredMethods.first()
            .answerableName().also {
                assert("public final <T> T foo(String, T)" == it)
            }
        "examples.classdesign.publicapi.methods.Varargs".load().declaredMethods.first()
            .answerableName().also {
                assert("public void multi(int, int...)" == it)
            }
    }
}
