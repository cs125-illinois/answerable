package edu.illinois.cs.cs125.answerable.api

interface BytecodeProvider {
    fun getBytecode(clazz: Class<*>): ByteArray
}