package edu.illinois.cs.cs125.answerable.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.unsecuredEnvironment
import java.util.Properties
import kotlin.random.Random
import mu.KotlinLogging

@Suppress("UNUSED")
private val logger = KotlinLogging.logger {}

val VERSION: String = Properties().also {
    it.load((object {}).javaClass.getResourceAsStream("/edu.illinois.cs.cs125.answerable.cli.version"))
}.getProperty("version")

class Answerable : CliktCommand() {
    override fun run() = Unit
}

class Version : CliktCommand(help = "print version and exit") {
    override fun run() {
        echo(VERSION)
    }
}

class Check : CliktCommand(help = "check submission against solution") {
    private val submissionClass: String by argument(
        name = "submissionClass",
        help = "class name of submission to test"
    )
    private val solutionClass: String by argument(
        name = "solutionClass",
        help = "class name of solution to test submission against"
    )
    private val solutionName: String by option(
        help = "solution name to use when solution class contains multiple solutions"
    ).default("")
    private val randomSeed: Long by option(
        help = "random seed to use for test generation"
    ).long().default(Random.nextLong())

    override fun run() {
        TestGenerator(getSolutionClass(solutionClass), solutionName)
            .loadSubmission(getAttemptClass(submissionClass))
            .runTests(randomSeed, unsecuredEnvironment)
            .assertAllSucceeded()
    }
}

fun main(args: Array<String>) = Answerable()
    .subcommands(Version(), Check())
    .main(args)

private fun getSolutionClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find reference solution named $name."
)

private fun getAttemptClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find student attempt class named $name."
)

private fun findClass(name: String, failMsg: String): Class<*> {
    return try {
        Class.forName(name)
    } catch (unused: ClassNotFoundException) {
        error(failMsg)
    }
}

@Suppress("unused")
fun assert(block: () -> String): Nothing {
    throw AssertionError(block())
}

@Suppress("unused")
fun check(block: () -> String): Nothing {
    throw IllegalStateException(block())
}

@Suppress("unused")
fun require(block: () -> String): Nothing {
    throw IllegalArgumentException(block())
}
