package edu.illinois.cs.cs125.answerablewebdemo


import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.lang.IllegalStateException

fun main() {

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