package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.EnumerableBytecodeLoader

class InvertedClassloader(private val klass: String) : ClassLoader() {
    override fun loadClass(name: String): Class<*> {
        return if (name == klass) {
            // Force resolution to continue downward, regardless of the contents of parent Classloaders
            throw ClassNotFoundException()
        } else {
            super.loadClass(name)
        }
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name == klass) {
            // Force resolution to continue downward, regardless of the contents of parent Classloaders
            throw ClassNotFoundException()
        } else {
            super.loadClass(name, resolve)
        }
    }
}

/**
 * Lots of modifications done in this file affect the bytecode of a class, so we need a way to hot-reload those classes
 * and get them back into the JVM. Here we can take a bytearray (class file) and load it.
 */
internal open class BytesClassLoader(parentLoader: ClassLoader? = null) :
    ClassLoader(parentLoader ?: getSystemClassLoader()), EnumerableBytecodeLoader {
    private val bytecodeLoaded = mutableMapOf<Class<*>, ByteArray>()
    private val definedClasses = mutableMapOf<String, Class<*>>()
    fun loadBytes(name: String, bytes: ByteArray): Class<*> {
        return definedClasses.getOrPut(name) {
            defineClass(name, bytes, 0, bytes.size).also { bytecodeLoaded[it] = bytes }
        }
    }

    override fun getBytecode(clazz: Class<*>): ByteArray {
        return bytecodeLoaded[clazz]
            ?: throw ClassNotFoundException("This BytesClassLoader is not responsible for $clazz")
    }

    override fun getAllBytecode(): Map<String, ByteArray> {
        return bytecodeLoaded.map { (key, value) -> key.name to value }.toMap()
    }

    override fun getLoader(): ClassLoader {
        return this
    }
}

/**
 * Like [BytesClassLoader], but also can ask other classloaders for classes. Useful in sandboxes.
 */
internal class DiamondClassLoader(
    primaryParent: ClassLoader,
    private vararg val otherParents: ClassLoader
) : BytesClassLoader(primaryParent) {
    override fun loadClass(name: String?): Class<*> {
        try {
            return super.loadClass(name)
        } catch (e: ClassNotFoundException) {
            otherParents.forEach {
                try {
                    return it.loadClass(name)
                } catch (ignored: ClassNotFoundException) {
                }
            }
        }
        throw ClassNotFoundException(name)
    }
}
