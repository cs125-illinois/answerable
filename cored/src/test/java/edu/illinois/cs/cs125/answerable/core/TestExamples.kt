package edu.illinois.cs.cs125.answerable.core

import io.github.classgraph.ClassGraph
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

@Suppress("RemoveSingleExpressionStringTemplate")
class TestExamples : StringSpec({
    examples.noreceiver.single.noarguments.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
    examples.receiver.single.noarguments.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
    examples.noreceiver.single.intargument.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
    examples.noreceiver.single.prints.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
    examples.noreceiver.single.submissionprints.Correct::class.java.also {
        "${it.testName()}" { it.test() }
    }
})

fun Class<*>.test() {
    solution(this).apply {
        submission(solution).test()
        ClassGraph().acceptPackages(packageName).scan().apply {
            allClasses
                .filter { it.simpleName != "Correct" && it.simpleName.startsWith("Correct") }
                .forEach { correct ->
                    submission(correct.loadClass()).test()
                }
            allClasses
                .filter { it.simpleName.startsWith("Incorrect") }
                .apply {
                    check(isNotEmpty()) { "No incorrect examples for ${testName()}" }
                }.forEach { incorrect ->
                    shouldThrow<Exception> {
                        submission(incorrect.loadClass()).test()
                    }
                }
        }
    }
}