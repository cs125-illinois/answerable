package edu.illinois.cs.cs125.answerable.api

/**
 * An interface that allows Answerable to look up the bytecode for classes under test.
 *
 * Applications must implement this interface if testing a class that does not exist on
 * disk in the application classpath.
 */
@FunctionalInterface
interface BytecodeProvider {

    /**
     * Looks up the bytecode used to create the class with [ClassLoader.defineClass].
     *
     * If this provider is not responsible for [clazz], this method should throw [NoClassDefFoundError].
     *
     * @param clazz a class under test
     * @return a byte array representing the compiled class
     */
    fun getBytecode(clazz: Class<*>): ByteArray

}

fun bytecodeProvider(block: (Class<*>) -> ByteArray) = object : BytecodeProvider {
    override fun getBytecode(clazz: Class<*>): ByteArray = block(clazz)
}
