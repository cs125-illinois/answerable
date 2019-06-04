package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test

internal class AdHoc {
    @Test
    fun test() {
        mkGeneratorMirrorClass(examples.proxy.reference.Widget::class.java, examples.proxy.Widget::class.java)
    }
}