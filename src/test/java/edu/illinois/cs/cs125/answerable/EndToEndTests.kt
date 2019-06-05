package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeoutException

class EndToEndTests {

    @Test
    fun testLastTenCorrect() {
        main(arrayOf("LastTen", "examples.lastten.correct."))
    }

    @Test
    fun testTimeOut() {
        assertThrows<TimeoutException> { main(arrayOf("TimeOut", "examples.testgeneration.timeout.")) }
    }

}
