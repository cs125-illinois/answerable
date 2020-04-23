package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.main
import org.junit.jupiter.api.Test

class EndToEndTests {

    @Test
    fun testLastTenCorrect() {
        main(arrayOf("LastTen", "", "examples.lastten.correct."))
    }
}
