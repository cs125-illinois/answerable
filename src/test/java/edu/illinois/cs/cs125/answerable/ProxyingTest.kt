package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.typeManagement.mkGeneratorMirrorClass
import examples.proxy.GeneratedWidget
import examples.proxy.reference.InnerClassGeneratorWidget
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ProxyingTest {

    fun assertAllSucceeded(results: TestRunOutput) {
        results.testSteps.forEach {
            Assertions.assertNull(it.assertErr)
            Assertions.assertTrue(it.succeeded)
        }
    }

    @Test
    fun testCorrectWidget() {
        val tg = TestRunner(examples.proxy.reference.Widget::class.java, examples.proxy.Widget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testCorrectFieldWidget() {
        val tg = TestRunner(examples.proxy.reference.FieldWidget::class.java, examples.proxy.FieldWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testGeneratedWidget() {
        val tg = TestRunner(examples.proxy.reference.GeneratedWidget::class.java, examples.proxy.GeneratedWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testCorrectHelperWidget() {
        val tg = TestRunner(examples.proxy.reference.HelperWidget::class.java, examples.proxy.HelperWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testComplexGeneratorWidget() {
        val tg = TestRunner(examples.proxy.reference.ComplexGeneratorWidget::class.java, examples.proxy.ComplexGeneratorWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testInnerClassGeneratorWidget() {
        val tg = TestRunner(examples.proxy.reference.InnerClassGeneratorWidget::class.java, examples.proxy.GeneratedWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testMultipleMirrors() {
        val first = mkGeneratorMirrorClass(InnerClassGeneratorWidget::class.java, GeneratedWidget::class.java)
        val second = mkGeneratorMirrorClass(InnerClassGeneratorWidget::class.java, GeneratedWidget::class.java)
        Assertions.assertNotEquals(first, second)
    }

}