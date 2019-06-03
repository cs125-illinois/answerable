package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test

internal class ProxyingTest {
    @Test
    fun testCorrectWidget() {
        main(arrayOf("Widget", "example.proxy"))
    }
}