package edu.illinois.cs.cs125.answerablewebdemo

import edu.illinois.cs.cs125.answerable.*
import edu.illinois.cs.cs125.answerable.jeedrunner.answerableBytecodeProvider
import edu.illinois.cs.cs125.answerable.jeedrunner.jeedOutputCapturer
import edu.illinois.cs.cs125.answerable.jeedrunner.jeedSandbox
import edu.illinois.cs.cs125.jeed.core.*
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.random.Random

// Essentially this entire module is a TODO

fun main1() {

    val server = embeddedServer(Netty, 8080 /*System.getenv("ANSWERABLE_DEMO_PORT")?.toInt()
        ?: throw IllegalStateException("ANSWERABLE_DEMO_PORT not provided.")*/) {

        install(ContentNegotiation) {
            jackson()
        }
        routing {
            options("/") {
                call.response.header("Access-Control-Allow-Origin", "*") // don't deploy this
                call.response.header("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token")
                call.response.status(HttpStatusCode.OK)
            }

            post("/") {
                call.response.header("Access-Control-Allow-Origin", "*") // don't deploy this
                val received = call.receive<AnswerableDemoPost>()
                println(received)
                call.response.status(HttpStatusCode.OK)
            }
        }
    }

    server.start(wait = true)

}

data class AnswerableDemoPost(
    val referenceString: String,
    val submissionString: String,
    val commonString: String,
    val solutionName: String
)

// TODO: Take advantage of the jeedrunner service
private fun main() {
    val commonSource = Source(mapOf(
            "Common.java" to """
public class Common {
    public static int identity(int number) {
        return number;
    }
}
            """.trim()
    ))

    val referenceSource = Source(mapOf(
        "Reference.java" to """
import edu.illinois.cs.cs125.answerable.api.*;

public class Test {
    @Solution
    public static int sum(int a, int b) {
        return a + Common.identity(b);
    }
}
        """.trim()
    ))
    val submissionSource = Source(mapOf(
        "Submission.java" to """
public class Test {
    public static int sum(int first, int second) {
        return Common.identity(first) - second;
    }
}
        """.trim()
    ))

    val common = try {
        commonSource.compile()
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n") }
        throw e
    }
    val refCL = try {
        referenceSource.compile(CompilationArguments(parentClassLoader = common.classLoader, parentFileManager = common.fileManager))
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n") }
        throw e
    }.classLoader
    val subCL = try {
        submissionSource.compile(CompilationArguments(parentClassLoader = common.classLoader, parentFileManager = common.fileManager))
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n") }
        throw e
    }.classLoader

    println(
        TestGenerator(refCL.loadClass("Test"), bytecodeProvider = answerableBytecodeProvider(refCL))
            .loadSubmission(subCL.loadClass("Test"), bytecodeProvider = answerableBytecodeProvider(subCL))
            .runTests(Random.nextLong(), TestEnvironment(jeedOutputCapturer, jeedSandbox()))
            .toJson()
    )

    Sandbox.shutdownThreadPool()
}
