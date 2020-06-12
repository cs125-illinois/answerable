package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.ossify
import edu.illinois.cs.cs125.answerable.typeManagement.TypePool
import edu.illinois.cs.cs125.answerable.typeManagement.mkOpenMirrorClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.isAccessible

private data class SimpleObject(val n: Int) {
    override fun toString(): String = "I hold $n"
}

private class ExplosiveObject {
    override fun toString(): String = error("Kaboom!")
}

private class HazardousObject {
    override fun toString(): String = throw object : RuntimeException() {
        override val message: String
            get() = error("Bang!")
    }
}

class OssificationTest {

    private lateinit var typePool: TypePool

    @BeforeEach
    fun setup() {
        typePool = TypePool(bytecodeProvider = null)
    }

    @Test
    fun testPrimitive() {
        val ossified = 125.ossify(typePool)!!
        assertEquals("java.lang.Integer", ossified.type)
        assertEquals("125", ossified.value)
    }

    @Test
    fun testNull() {
        assertNull(null.ossify(typePool))
    }

    @Test
    fun testObject() {
        val ossified = SimpleObject(323).ossify(typePool)!!
        assertEquals("edu.illinois.cs.cs125.answerable.SimpleObject", ossified.type)
        assertEquals("I hold 323", ossified.value)
    }

    @Test
    fun testPrimitiveArray() {
        val ossified = intArrayOf(1, 2, 5).ossify(typePool)!!
        assertEquals("int[]", ossified.type)
        assertEquals("[1, 2, 5]", ossified.value)
    }

    @Test
    fun testObjectArray() {
        val ossified = arrayOf("a", "b", "c").ossify(typePool)!!
        assertEquals("java.lang.String[]", ossified.type)
        assertEquals("[a, b, c]", ossified.value)
    }

    @Test
    fun testNestedArray() {
        val ossified = arrayOf(arrayOf("w", "x"), arrayOf("y", "z")).ossify(typePool)!!
        assertEquals("java.lang.String[][]", ossified.type)
        assertEquals("[[w, x], [y, z]]", ossified.value)
    }

    @Test
    fun testIdentity() {
        val one = SimpleObject(100)
        val two = SimpleObject(100)
        assertEquals(one, two)
        assertNotEquals(one.ossify(typePool)!!.identity, two.ossify(typePool)!!.identity)
    }

    @Test
    fun testExplosiveStringify() {
        val ossified = ExplosiveObject().ossify(typePool)!!
        assertEquals("edu.illinois.cs.cs125.answerable.ExplosiveObject", ossified.type)
        assertEquals("<failed to stringify ${ExplosiveObject::class.java.name}: Kaboom!>", ossified.value)
    }

    @Test
    fun testDoubleFaultingStringify() {
        val ossified = HazardousObject().ossify(typePool)!!
        assertEquals("edu.illinois.cs.cs125.answerable.HazardousObject", ossified.type)
        assertEquals("<stringification of ${HazardousObject::class.java.name} double-faulted>", ossified.value)
    }

    @Test
    fun testExplosiveArrayEntry() {
        val ossified = arrayOf(ExplosiveObject()).ossify(typePool)!!
        assertEquals("edu.illinois.cs.cs125.answerable.ExplosiveObject[]", ossified.type)
        assertEquals("<failed to stringify ${ExplosiveObject::class.java.name}: Kaboom!>", ossified.value)
    }

    @Test
    fun testOriginalName() {
        val mirrorClass = mkOpenMirrorClass(SimpleObject::class.java, typePool)
        val mirrorCtor = mirrorClass.kotlin.constructors.first { it.parameters.size == 1 }
        mirrorCtor.isAccessible = true
        val ossified = mirrorCtor.call(1701).ossify(typePool)!!
        assertEquals(SimpleObject::class.qualifiedName, ossified.type)
        assertEquals("I hold 1701", ossified.value)
    }
}
