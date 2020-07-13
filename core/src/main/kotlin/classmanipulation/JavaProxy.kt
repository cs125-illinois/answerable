package edu.illinois.cs.cs125.answerable.classmanipulation

import edu.illinois.cs.cs125.answerable.annotations.answerableInterface
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal fun makeValueProxy(referenceClass: Class<*>, submissionClass: Class<*>, obj: Any?): Any? =
    when {
        obj == null -> null
        obj.javaClass == referenceClass || obj.javaClass == submissionClass ->
            referenceClass.makeJavaProxy(obj)
        else -> obj
    }

/**
 * The returned dynamic proxy object is defined by the same classloader that defined
 * the answerable reference class.
 *
 * @receiver the answerable reference class being proxied to
 * @param [obj] the object to proxy
 */
internal fun Class<*>.makeJavaProxy(obj: Any?): Any? {
    if (obj == null) return null
    val iface = this.answerableInterface
    return Proxy.newProxyInstance(
        this.classLoader,
        arrayOf(iface),
        AnswerableInvocationHandler(this, obj)
    )
}

private class AnswerableInvocationHandler(val answerableInterface: Class<*>, val underlyingObject: Any) : InvocationHandler {
    val clazz = underlyingObject.javaClass
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>): Any {
        val argTypes: Array<Class<*>> =
            method.parameterTypes
                .map { if (it == answerableInterface) clazz else it }
                .toTypedArray()
        val objMethod = clazz.getMethod(method.name, *argTypes)

        val unproxiedArgs = args
            .map(::unProxy)
            .toTypedArray()
        return objMethod(unProxy(proxy), *unproxiedArgs)
    }
}

private fun unProxy(obj: Any?): Any? {
    if (obj == null) return null

    if (obj is Proxy) {
        val ih = Proxy.getInvocationHandler(obj)
        if (ih is AnswerableInvocationHandler) {
            return ih.underlyingObject
        }
    }

    return obj
}
