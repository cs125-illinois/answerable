package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ProxyingTest {

    fun assertAllSucceeded(results: Collection<TestStep>) {
        results.forEach {
            Assertions.assertNull(it.assertErr)
            Assertions.assertTrue(it.succeeded)
        }
    }

    @Test
    fun testCorrectWidget() {
        val tg = TestGenerator(examples.proxy.reference.Widget::class.java, examples.proxy.Widget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testCorrectFieldWidget() {
        val tg = TestGenerator(examples.proxy.reference.FieldWidget::class.java, examples.proxy.FieldWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testGeneratedWidget() {
        val tg = TestGenerator(examples.proxy.reference.GeneratedWidget::class.java, examples.proxy.GeneratedWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

}