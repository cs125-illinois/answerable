package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.answerableParams
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.getEnabledEdgeCases
import edu.illinois.cs.cs125.answerable.getEnabledSimpleCases
import edu.illinois.cs.cs125.answerable.languageMode
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.Random

enum class CaseKind {
    EDGE, SIMPLE;

    override fun toString(): String = name.toLowerCase() + " case"
}

// Sadly not an Iterator because `next` needs a Random instance
// but it's outside the scope of the class to capture one.
class MethodArgumentCases internal constructor(
    private val caseMap: CaseMap,
    private val backupGenerators: GeneratorMapV2?,
    val method: Method
) {
    val caseKind: CaseKind = caseMap.caseKind

    private val parameters = method.answerableParams
    private val parameterTypes = parameters.map { it.type }
    private val parameterCases = parameterTypes.map { caseMap[it] }
    private val parameterCaseCounts = parameterCases.map { it?.size ?: 1 }
    private val numCases = parameterTypes
        .map { caseMap[it] }
        .map { it?.size ?: 1 }
        .foldRight(1) { x, y -> x * y }

    init {
        val unsatisfied = method.answerableParams.filter { param: GeneratorRequest ->
            !(caseMap.containsKey(param.type) || (backupGenerators?.containsKey(param) ?: false))
        }
        if (unsatisfied.isNotEmpty()) {
            throw AnswerableMisuseException(
                "Can't satisfy $caseKind requests: $unsatisfied"
            )
        }
    }

    /* Gets a particular combination of the cases for each argument, AKA
     * one "argument case." All of the possible cases can be indexed. Consider
     * the following small example;
     *
     *    Int | Int
     * 0 | -1 | -1
     * 1 |  0 | -1
     * 2 |  1 | -1
     * 3 | -1 |  0
     * 4 |  0 |  0
     * ...
     *
     * Calculating which case belongs to a given index is a lot like decomposing
     * the decimal representation of an integer value, except that each "digit"
     * may have a different "base."
     *
     * We can't just store all the cases and pull from that because of combinatorial
     * explosion.
     *
     * Only call this function with 0 <= caseIndex < numCases.
     */
    private fun getCase(caseIndex: Int, random: Random): Array<Any?> {
        var toDecompose = caseIndex
        val digits = mutableListOf<Int>()
        for (b in parameterCaseCounts) {
            digits += toDecompose % b
            toDecompose /= b
        }

        val chosenCases = parameterTypes.indices
            .map { i ->
                val param = parameters[i]
                val type = param.type
                caseMap[type]?.get(digits[i])
                    ?: backupGenerators?.get(param)?.generate(0, random)
            }
        return chosenCases.toTypedArray()
    }

    /* User is doing something questionable if this gets much bigger than 4^4 == 256 */
    private val caseIndices = (0 until numCases).toMutableList()
    private var numCalls = 0
    fun reset() {
        for (i in caseIndices.indices) {
            caseIndices[i] = i
        }
        numCalls = 0
    }
    fun hasNext(): Boolean = numCalls < numCases
    /** A Fisher-Yates shuffler */
    private fun swap(i: Int, j: Int) {
        val temp = caseIndices[i]
        caseIndices[i] = caseIndices[j]
        caseIndices[j] = temp
    }
    private fun nextCaseIndex(random: Random): Int {
        val j = numCalls + random.nextInt(numCases - numCalls)
        swap(numCalls, j)
        return caseIndices[numCalls].also { numCalls++ }
    }

    fun nextCase(random: Random): Array<Any?> = getCase(nextCaseIndex(random), random)
}

internal data class CaseMap(
    private val map: Map<Type, Array<out Any?>>,
    val caseKind: CaseKind
) : Map<Type, Array<out Any?>> by map {
    fun casesForMethod(method: Method, backupGenerators: GeneratorMapV2? = null): MethodArgumentCases =
        MethodArgumentCases(this, backupGenerators, method)
}

internal fun Class<*>.edgeCaseMap(
    pool: TypePool = TypePool(),
    controlClass: Class<*>? = null
) = this.buildCaseMap(CaseKind.EDGE, pool, controlClass)
internal fun Class<*>.simpleCaseMap(
    pool: TypePool = TypePool(),
    controlClass: Class<*>? = null
) = this.buildCaseMap(CaseKind.SIMPLE, pool, controlClass)

private fun Class<*>.buildCaseMap(
    caseKind: CaseKind,
    pool: TypePool,
    controlClass: Class<*>?
): CaseMap {
    val usableControlClass = controlClass
        ?: this.languageMode().findControlClass(this, pool)
        ?: this

    val cases = when (caseKind) {
        CaseKind.EDGE -> this.languageMode().defaultEdgeCases + usableControlClass.getEnabledEdgeCases(arrayOf())
        CaseKind.SIMPLE -> this.languageMode().defaultSimpleCases + usableControlClass.getEnabledSimpleCases(arrayOf())
    }

    val unwrappedCases = cases.mapValues { (_, arr) -> arr.unwrap() }
    return CaseMap(unwrappedCases, caseKind)
}