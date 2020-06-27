import edu.illinois.cs.cs125.answerable.core.Answerable
import edu.illinois.cs.cs125.answerable.core.answerable
import io.github.classgraph.ClassGraph
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

@Suppress("RemoveSingleExpressionStringTemplate")
class `TestExamples` : StringSpec({
    examples.singlestaticmethodnoarguments.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
    examples.singlemethodnoarguments.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
    examples.singlestaticmethodsingleintargument.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
})

fun Class<*>.test() {
    answerable().also { answerable ->
        answerable.test(this)
        ClassGraph().acceptPackages(packageName).scan().apply {
            allClasses
                .filter { it.simpleName != "Correct" && it.simpleName.startsWith("Correct") }
                .forEach { correct ->
                    answerable.test(correct.loadClass())
                }
            allClasses
                .filter { it.simpleName.startsWith("Incorrect") }
                .apply {
                    check(isNotEmpty()) { "No incorrect examples for ${testName()}" }
                }.forEach { incorrect ->
                    shouldThrow<Exception> {
                        answerable.test(incorrect.loadClass())
                    }
                }
        }
    }
}

fun Class<*>.testName() = packageName.removePrefix("examples.")
