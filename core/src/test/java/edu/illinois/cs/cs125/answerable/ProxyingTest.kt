package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.classmanipulation.mkGeneratorMirrorClass
import edu.illinois.cs.cs125.answerable.classmanipulation.mkOpenMirrorClass
import edu.illinois.cs.cs125.jeed.core.CompilationArguments
import edu.illinois.cs.cs125.jeed.core.JeedClassLoader
import edu.illinois.cs.cs125.jeed.core.Source
import edu.illinois.cs.cs125.jeed.core.compile
import examples.proxy.GeneratedWidget
import examples.proxy.reference.InnerClassGeneratorWidget
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class ProxyingTest {

    fun assertAllSucceeded(results: TestingResults) {
        results.testSteps.forEach {
            if (it is ExecutedTestStep) {
                Assertions.assertNull(it.assertErr)
                Assertions.assertTrue(it.succeeded)
            }
        }
    }

    @Test
    fun testCorrectWidget() {
        val tg = PassedClassDesignRunner(examples.proxy.reference.Widget::class.java, examples.proxy.Widget::class.java)
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testCorrectFieldWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.FieldWidget::class.java,
            examples.proxy.FieldWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    @Disabled("proxying of public fields is postponed indefinitely - failure expected")
    fun testCorrectFieldLinkedList() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.FieldLinkedList::class.java,
            examples.proxy.FieldLinkedList::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testGeneratedWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.GeneratedWidget::class.java,
            examples.proxy.GeneratedWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testCorrectHelperWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.HelperWidget::class.java,
            examples.proxy.HelperWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testComplexGeneratorWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.ComplexGeneratorWidget::class.java,
            examples.proxy.ComplexGeneratorWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testInnerClassGeneratorWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.InnerClassGeneratorWidget::class.java,
            examples.proxy.GeneratedWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testMultipleMirrors() {
        val first = mkGeneratorMirrorClass(InnerClassGeneratorWidget::class.java, GeneratedWidget::class.java)
        val second = mkGeneratorMirrorClass(InnerClassGeneratorWidget::class.java, GeneratedWidget::class.java)
        Assertions.assertNotEquals(first, second)
    }

    @Test
    fun testStaticInitWidget() {
        // CAUTION: There isn't really a viable way to mirror just the parts of <clinit> that have to do with generation.
        // This test only works because the static initializer isn't involved in implementation details.
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.StaticInitGeneratorWidget::class.java,
            examples.proxy.StaticInitGeneratorWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testRequiredInnerClassWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.RequiredInnerClassWidget::class.java,
            examples.proxy.RequiredInnerClassWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testFinalInnerClassWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.FinalRequiredInnerClassWidget::class.java,
            examples.proxy.FinalRequiredInnerClassWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testInstanceofInnerClassWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.InstanceofInnerClassWidget::class.java,
            examples.proxy.InstanceofInnerClassWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testUnknownSubclassInnerClassWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.InstanceofInnerClassWidget::class.java,
            examples.proxy.SubclassedInnerClassWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testCollidingSubclassInnerClassWidget() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.CollidingInnerClassWidget::class.java,
            examples.proxy.CollidingInnerClassWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testVerifyArgsProxying() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.WidgetArgumentWidget::class.java,
            examples.proxy.WidgetArgumentWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testReceiverCallArgsProxying() {
        val tg = PassedClassDesignRunner(
            examples.proxy.reference.CopyableWidget::class.java,
            examples.proxy.CopyableWidget::class.java
        )
        assertAllSucceeded(tg.runTestsUnsecured(0x0403))
    }

    @Test
    fun testMirroringClassOnSystemClasspath() {
        // Depends on examples.proxy.Printerr existing on the class path
        val jeedClass = Source(
            mapOf(
                "Printerr.java" to """
                    package examples.proxy;
    
                    public class Printerr {
                        public static String welcome() {
                            return "Jeed";
                        }
                    }
                """.trimIndent()
            )
        ).compile(CompilationArguments(parentClassLoader = InvertedClassloader("examples.proxy.Printerr")))
            .classLoader.loadClass("examples.proxy.Printerr")

        val bytecodeProvider = object : BytecodeProvider {
            override fun getBytecode(clazz: Class<*>): ByteArray {
                return (jeedClass.classLoader as JeedClassLoader).bytecodeForClass(clazz.name)
            }
        }
        Assertions.assertEquals("Jeed", jeedClass.getDeclaredMethod("welcome").invoke(null))
        val mirroredClass = mkOpenMirrorClass(jeedClass, TypePool(bytecodeProvider, jeedClass.classLoader))
        Assertions.assertEquals("Jeed", mirroredClass.getDeclaredMethod("welcome").invoke(null))
    }
}
