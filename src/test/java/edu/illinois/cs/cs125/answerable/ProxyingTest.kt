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
        val first = mkGeneratorMirrorClass(examples.proxy.reference.InnerClassGeneratorWidget::class.java, examples.proxy.GeneratedWidget::class.java)
        val second = mkGeneratorMirrorClass(examples.proxy.reference.InnerClassGeneratorWidget::class.java, examples.proxy.GeneratedWidget::class.java)
        Assertions.assertNotEquals(first, second)
    }

    @Test
    fun testStaticInitWidget() {
        // CAUTION: There isn't really a viable way to mirror just the parts of <clinit> that have to do with generation.
        // This test only works because the static initializer isn't involved in implementation details.
        val tg = TestRunner(examples.proxy.reference.StaticInitGeneratorWidget::class.java, examples.proxy.StaticInitGeneratorWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testRequiredInnerClassWidget() {
        val tg = TestRunner(examples.proxy.reference.RequiredInnerClassWidget::class.java, examples.proxy.RequiredInnerClassWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

    @Test
    fun testFinalInnerClassWidget() {
        val tg = TestRunner(examples.proxy.reference.FinalRequiredInnerClassWidget::class.java, examples.proxy.FinalRequiredInnerClassWidget::class.java)
        assertAllSucceeded(tg.runTests(0x0403))
    }

}