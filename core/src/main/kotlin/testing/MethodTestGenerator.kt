package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import java.lang.reflect.Method

internal class MethodTestGenerator internal constructor(
    internal val edgeCases: MethodArgumentCases,
    internal val simpleCases: MethodArgumentCases,
    internal val generator: MethodArgumentGenerator,
    val method: Method
) {
    /**
     * Generate an input case for the given testing block.
     *
     * Some internal state is maintained for edge and simple cases.
     */
    fun generate(): Array<Any?> = TODO("Implement MethodTestGenerator.generate")
}

internal class MethodTestGeneratorFactory internal constructor(
    klass: Class<*>,
    pool: TypePool = TypePool(),
    controlClass: Class<*>? = null
) {
    private val edgeCaseMap = klass.edgeCaseMap(pool, controlClass)
    private val simpleCaseMap = klass.simpleCaseMap(pool, controlClass)
    private val generatorMap = klass.generatorMap(pool, controlClass)

    fun makeFor(method: Method): MethodTestGenerator =
        MethodTestGenerator(
            edgeCaseMap.casesForMethod(method, generatorMap),
            simpleCaseMap.casesForMethod(method, generatorMap),
            generatorMap.generatorForMethod(method),
            method
        )
}

sealed class TestBlock {
    object EdgeCase : TestBlock()
    object SimpleCase : TestBlock()
    class Generated(val complexity: Int) : TestBlock()
}