package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ProxyingTest {

    @Test
    fun testCorrectWidget() {
        val tg = TestGenerator(examples.proxy.reference.Widget::class.java, examples.proxy.Widget::class.java)
        val results = tg.runTests(0x0403)
        Assertions.assertTrue(results[0].succeeded)
        Assertions.assertNull(results[0].assertErr)
    }

    @Test
    fun testCorrectFieldWidget() {
        val tg = TestGenerator(examples.proxy.reference.FieldWidget::class.java, examples.proxy.FieldWidget::class.java)
        val results = tg.runTests(0x0403)
        Assertions.assertTrue(results[0].succeeded)
        Assertions.assertNull(results[0].assertErr)
    }

    @Test
    fun testGeneratedWidget() {
        val tg = TestGenerator(examples.proxy.reference.GeneratedWidget::class.java, examples.proxy.GeneratedWidget::class.java)
        val results = tg.runTests(0x0403)
        Assertions.assertTrue(results[0].succeeded)
        Assertions.assertNull(results[0].assertErr)
    }

}