package edu.illinois.cs.cs125.answerable

import javassist.util.proxy.Proxy
import javassist.util.proxy.ProxyFactory
import org.objenesis.ObjenesisStd

fun mkProxy(superClass: Class<*>, childClass: Class<*>, forward: Any?): Any {
    val factory = ProxyFactory()
    val objenesis = ObjenesisStd()

    factory.superclass = superClass
    factory.setFilter { it.name != "finalize" }
    val proxyClass = factory.createClass()

    val instantiator = objenesis.getInstantiatorOf(proxyClass)
    val subProxy = instantiator.newInstance()

    (subProxy as Proxy).setHandler { _, method, _, args ->
        childClass.getMethod(method.name, *method.parameterTypes).invoke(forward, *args)
    }

    return subProxy
}
