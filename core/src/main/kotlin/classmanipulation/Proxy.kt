package edu.illinois.cs.cs125.answerable.classmanipulation

import edu.illinois.cs.cs125.answerable.publicFields
import javassist.util.proxy.Proxy
import java.lang.reflect.Modifier

/**
 * Creates a proxy to allow treating an object as an instance of a similarly-shaped class.
 * @param superClass the class that the object needs to appear as (be an instance of)
 * @param childClass the original class
 * @param forward an instance of childClass to which method calls will be forwarded
 * @param pool the type pool to look for classes in
 * @return an instance (the proxy) of a subclass of superClass
 */
internal fun mkProxy(superClass: Class<*>, childClass: Class<*>, forward: Any, pool: TypePool): Any {
    return mkProxy(
        superClass,
        superClass,
        childClass,
        childClass,
        forward,
        pool
    )
}

/**
 * A witness that instances of the "behavior" class should be proxied such that they can be used
 * as though they were instances of the "presentation" class.
 */
private data class TypeMapping(
    val behavior: Class<*>,
    val presentation: Class<*>
)

/**
 * The value for a proxied method call and the argument type from the method signature.
 * The concrete type of the argument may not be the same as the argument type.
 */
private data class TypedArgument(
    val type: Class<*>,
    val argument: Any?
)

/**
 * Determines what a class should be proxied to, if any.
 * @param outermostPresentationClass the outer class a proxy is being made an instance of
 * @param outermostBehaviorClass the outer class of the real/original objects
 * @param valueClass the class to potentially map
 * @param pool type pool to look for superclasses in
 */
private fun proxyableClass(
    outermostPresentationClass: Class<*>,
    outermostBehaviorClass: Class<*>,
    valueClass: Class<*>,
    pool: TypePool
): TypeMapping? {
    return when {
        Proxy::class.java.isAssignableFrom(valueClass) -> {
            proxyableClass(
                outermostPresentationClass,
                outermostBehaviorClass,
                valueClass.superclass,
                pool
            )
        }
        valueClass == outermostBehaviorClass -> {
            TypeMapping(
                behavior = outermostBehaviorClass,
                presentation = outermostPresentationClass
            )
        }
        valueClass.name.startsWith("${outermostBehaviorClass.name}$") -> {
            mostDerivedProxyableClass(
                outermostPresentationClass,
                valueClass,
                pool
            )
        }
        else -> {
            null
        }
    }
}

/* NOTE: [Proxy inheritance with inner classes]

Consider this example:

```
class Reference {
    public class Foo { }

    Foo getAFoo() {
        return new Foo();
    }
}

class Submission {
    public class Foo { }
    private class Bar extends Foo { }

    Foo getAFoo() {
        return new Bar();
    }
}
```

If we aren't careful, this will lead to trying to proxy instances of Submission.Bar as instances of Reference.Bar.
But Reference.Bar doesn't exist and that would be bad.

So instead, we find the nearest parent class which exists in Reference and is part of the contract,
in this case Reference.Foo, and proxy it as an instance of that.
 */
/**
 * Determines what an inner class should be proxied to, if any.
 *
 * This is a defense against some obscure corner case submissions, so that Answerable doesn't crash
 * (and also behaves correctly). See note [Proxy inheritance with inner classes]
 *
 * @param outermostSuperClass the outer class a proxy is being made an instance of
 * @param childClass an inner class of the real/original class
 * @param pool type pool to look for super classes in
 * @return a TypeMapping that determines what classes to map fields and method between, or null if no proxy is needed
 */
@Suppress("ReturnCount")
private fun mostDerivedProxyableClass(
    outermostSuperClass: Class<*>,
    childClass: Class<*>?,
    pool: TypePool
): TypeMapping? {
    if (childClass == null) return null
    if (childClass.enclosingClass == null) return null
    val innerPath = childClass.name.split('$', limit = 2)[1]
    val correspondingSuper = "${outermostSuperClass.name}\$$innerPath"
    val usableMatch = try {
        val match = pool.classForName(correspondingSuper)
        if (Modifier.isPrivate(match.modifiers)) null else
            TypeMapping(
                presentation = pool.classForName(
                    correspondingSuper
                ),
                behavior = childClass
            )
    } catch (e: ClassNotFoundException) {
        null
    }
    return usableMatch ?: mostDerivedProxyableClass(
        outermostSuperClass,
        childClass.superclass,
        pool
    )
}

/**
 * Returns an instance of presentationClass that behaves equivalently to behavior instance: method calls
 * on the returned instance are forwarded to the behaviorInstance, and modifications to fields of the returned proxy
 * are synced with behaviorInstance whenever a method is called on the returned proxy. Modifications to
 * behaviorInstance are not synced and will be clobbered when the proxy syncs.
 *
 * This function effectively invalidates behaviorInstance.
 * Any modifications to the object referenced by behaviorInstance induce undefined behavior.
 *
 * @param presentationClass The class which 'behaviorInstance' should be usable as after proxying
 * @param outermostPresentationClass The top-level class which contains presentation class as
 *     a (possibly deep) inner class. If presentationClass is a top-level class, should be presentationClass.
 * @param behaviorClass The class at which we should use behaviorInstance.
 * @param outermostBehaviorClass See outermostPresentationClass.
 * @param behaviorInstance The instance of behaviorClass which is being proxied.
 * @param pool A TypePool from which we can get superclasses of the behaviorClass.
 */
@Suppress("LongParameterList", "ReturnCount")
private fun mkProxy(
    presentationClass: Class<*>,
    outermostPresentationClass: Class<*>,
    behaviorClass: Class<*>,
    outermostBehaviorClass: Class<*>,
    behaviorInstance: Any,
    pool: TypePool
): Any {
    if (presentationClass == behaviorClass) return behaviorInstance
    pool.getProxyOriginal(behaviorInstance)?.takeIf { existingProxy ->
        presentationClass.isAssignableFrom(existingProxy.javaClass)
    }?.let { return it }

    val subProxy = pool.getProxyInstantiator(presentationClass).newInstance()
    (subProxy as Proxy).setHandler { self, method, _, args ->
        // sync out
        behaviorClass.publicFields.forEach { it.set(behaviorInstance, self.javaClass.getField(it.name).get(self)) }
        // proxy arguments the opposite direction for compatibility with the real object
        val arguments = method.parameterTypes.indices.map { i ->
            val argumentProxying = proxyableClass(
                outermostBehaviorClass, outermostPresentationClass,
                method.parameterTypes[i], pool
            )
            val proxiedArgumentType = argumentProxying?.presentation ?: method.parameterTypes[i]
            val argumentValue = mkValueProxy(
                args[i],
                outermostBehaviorClass,
                outermostPresentationClass,
                pool
            )
            TypedArgument(
                proxiedArgumentType,
                argumentValue
            )
        }
        // actual proxied method call
        val result = behaviorClass.getMethod(method.name, *arguments.map { it.type }.toTypedArray())
            .invoke(behaviorInstance, *arguments.map { it.argument }.toTypedArray())
        // sync in
        behaviorClass.publicFields.forEach { self.javaClass.getField(it.name).set(self, it.get(behaviorInstance)) }
        // return result proxied if necessary
        mkValueProxy(
            result,
            outermostPresentationClass,
            outermostBehaviorClass,
            pool
        )
    }

    pool.recordProxyOriginal(behaviorInstance, subProxy)
    return subProxy
}

/**
 * Proxies (if necessary) one value of an unknown type.
 * @param value the value to potentially proxy, may be null
 * @param outermostBehaviorClass the outermost real/original class
 * @param outermostPresentationClass the outermost class of the proxy
 * @param pool type pool to look for superclasses in
 */
@Suppress("LiftReturnOrAssignment", "ReturnCount")
internal fun mkValueProxy(
    value: Any?,
    outermostPresentationClass: Class<*>,
    outermostBehaviorClass: Class<*>,
    pool: TypePool
): Any? {
    if (value == null) return null
    val mapping = proxyableClass(
        outermostPresentationClass,
        outermostBehaviorClass,
        value.javaClass,
        pool
    )
        ?: return value
    val innerProxy = mkProxy(
        mapping.presentation, outermostPresentationClass,
        mapping.behavior, outermostBehaviorClass, value, pool
    )
    mapping.behavior.publicFields
        .forEach { innerProxy.javaClass.getField(it.name).set(innerProxy, it.get(value)) }
    return innerProxy
}
