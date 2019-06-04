package edu.illinois.cs.cs125.answerable

import kotlin.random.Random

fun main(args: Array<String>) {
    val name = args[0]

    val prefix = args.elementAtOrElse(1) {""} // for testing

    val reference = getSolutionClass("${prefix}reference.$name")
    val submission = getAttemptClass("$prefix$name")

    if (reference.isClassDesignReference()) {
        println(ClassDesignAnalysis(reference, submission).runSuite())
    }

    val tg = TestGenerator(reference, submission)

    println(tg.runTests(Random.nextLong()).toJson())
}