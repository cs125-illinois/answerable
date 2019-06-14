package edu.illinois.cs.cs125.answerablewebdemo

import edu.illinois.cs.cs125.answerable.api.Answerable
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import io.github.cdimascio.dotenv.*

import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.lang.IllegalStateException

val dotenv = dotenv { directory = "demo/" }

fun main() {

    val server = embeddedServer(Netty, dotenv["ANSWERABLE_DEMO_PORT"]?.toInt()
        ?: throw IllegalStateException("ANSWERABLE_DEMO_PORT not provided in .env")) {

        install(ContentNegotiation) {
            jackson { }
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