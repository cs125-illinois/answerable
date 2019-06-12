package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.*
import kotlin.random.Random

fun main(args: Array<String>) {
    val referenceName = args[0]
    val solutionName = args[1]

    val prefix = args.elementAtOrElse(2) {""} // for testing

    val reference = getSolutionClass("${prefix}reference.$referenceName")
    val submission = getAttemptClass("$prefix$referenceName")

    val tg = TestGenerator(reference, solutionName)
    val tr = tg.loadSubmission(submission)
    println(tr.runTests(Random.nextLong()).toJson())
}