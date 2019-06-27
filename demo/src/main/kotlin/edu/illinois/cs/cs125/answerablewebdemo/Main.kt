package edu.illinois.cs.cs125.answerablewebdemo


import edu.illinois.cs.cs125.answerable.*
import edu.illinois.cs.cs125.answerable.api.Solution
import edu.illinois.cs.cs125.jeed.core.*
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.lang.IllegalStateException
import kotlin.random.Random

fun main1() {

    val server = embeddedServer(Netty, System.getenv("ANSWERABLE_DEMO_PORT")?.toInt()
        ?: throw IllegalStateException("ANSWERABLE_DEMO_PORT not provided.")) {

        install(ContentNegotiation) {
            jackson()
        }
        routing {
            post("/") {
                val received = call.receive<AnswerableDemoPost>()
            }
        }
    }

    server.start(wait = true)

}

data class AnswerableDemoPost(
    val referenceString: String,
    val submissionString: String,
    val commonString: String
)

fun main() {
    val referenceSource = Source(mapOf(
        "Reference" to """
import edu.illinois.cs.cs125.answerable.api.*;

public class Test {
    @Solution
    public static int sum(int a, int b) {
        return a + b;
    }
}
        """.trim()
    ))
    val submissionSource = Source(mapOf(
        "Submission" to """
public class Test {
    public static int sum(int first, int second) {
        return first - second;
    }
}
        """.trim()
    ))

    val refCL = try {
        referenceSource.compile(CompilationArguments(parentClassLoader = Solution::class.java.classLoader))
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n")}
        throw e
    }.classLoader
    val subCL = try {
        submissionSource.compile()
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n")}
        throw e
    }.classLoader

    println(
        TestGenerator(refCL.loadClass("Test"), bytecodeProvider = answerableBytecodeProvider(refCL))
            .loadSubmission(subCL.loadClass("Test"), bytecodeProvider = answerableBytecodeProvider(subCL))
            .runTests(Random.nextLong(), TestEnvironment(jeedOutputCapturer, jeedSandbox()))
            .toJson()
    )

    // FIXME: The process doesn't exit, probably because of the Jeed ExecutorService not being shut down.
}
