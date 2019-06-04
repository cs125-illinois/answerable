package edu.illinois.cs.cs125.answerable

import javassist.util.proxy.Proxy
import javassist.util.proxy.ProxyFactory
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator

val objenesis = ObjenesisStd()

/*
 * For performance reasons, we want to re-use instantiators as much as possible.
 * A map is used for future-safety so that as many proxy instantiators as are needed can be created safely,
 * even if using only one is the most common use case.
 *
 * We map from 'superClass' instead of directly from 'proxyClass' as we won't have access to
 * the same reference to 'proxyClass' on future calls.
 */
val proxyInstantiators: MutableMap<Class<*>, ObjectInstantiator<out Any?>> = mutableMapOf()

fun mkProxy(superClass: Class<*>, childClass: Class<*>, forward: Any?): Any {
    // if we don't have an instantiator for this proxy class, make a new one
    val instantiator = proxyInstantiators[superClass] ?: run {
        val factory = ProxyFactory()

        factory.superclass = superClass
        factory.setFilter { it.name != "finalize" }
        val proxyClass = factory.createClass()

        val newInstantiator = objenesis.getInstantiatorOf(proxyClass)
        proxyInstantiators[superClass] = newInstantiator
        newInstantiator
    }
    val subProxy = instantiator.newInstance()

    (subProxy as Proxy).setHandler { _, method, _, args ->
        childClass.getMethod(method.name, *method.parameterTypes).invoke(forward, *args)
    }

    return subProxy
}
