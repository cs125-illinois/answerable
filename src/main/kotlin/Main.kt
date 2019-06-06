package edu.illinois.cs.cs125.answerable

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun main(args: Array<String>) {
    val name = args[0]

    val prefix = args.elementAtOrElse(1) {""} // for testing

    val reference = getSolutionClass("${prefix}reference.$name")
    val submission = getAttemptClass("$prefix$name")

    val answerable =
        {
            println(ClassDesignAnalysis(reference, submission).runSuite().toJson())
            val tg = TestRunner(reference, submission)
            println(tg.runTests(Random.nextLong()).toJson())
        }

    val timeout = reference.getAnnotation(Timeout::class.java)?.timeout ?: 0

    if (timeout == 0L) {
        answerable()
    } else {
        val executor = Executors.newSingleThreadExecutor()
        executor.submit(answerable)[timeout, TimeUnit.MILLISECONDS]
    }
}