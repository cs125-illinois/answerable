package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.ArrayWrapper
import edu.illinois.cs.cs125.answerable.LanguageMode
import edu.illinois.cs.cs125.answerable.annotations.Pair
import edu.illinois.cs.cs125.answerable.annotations.getAllGenerators
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.sourceName
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.Random
import kotlin.math.min

// NOTE: [Generator Keys]
// goalTypes holds types that we need generators for. @UseGenerator annotations allow specifying a specific generator.
// The string in the Pair is non-null iff a specific generator is requested.
internal class GeneratorMapBuilder(
    goalTypes: kotlin.Array<GeneratorType>,
    private val random: Random,
    private val pool: TypePool,
    private val receiverType: Class<*>?,
    languageMode: LanguageMode
) {
    private var knownGenerators: MutableMap<GeneratorType, Lazy<Gen<*>>> = mutableMapOf()
    private val defaultGenerators: Map<GeneratorType, Gen<*>> =
        languageMode.defaultGenerators.mapKeys { (klass, _) -> GeneratorType(klass) }

    init {
        defaultGenerators.forEach { (k, v) -> accept(k, v) }
        knownGenerators[String::class.java.asGeneratorType()] = lazy {
            DefaultStringGen(
                knownGenerators[Char::class.java.asGeneratorType()]!!.value
            )
        }
    }

    private val requiredGenerators: Set<GeneratorType> = goalTypes.toSet().also { it.forEach(this::request) }

    private fun lazyGenError(type: Type) =
        AnswerableMisuseException(
            "A generator for type `${pool.getOriginalClass(type).sourceName}' was requested, " +
                "but no generator for that type was found."
        )

    private fun lazyArrayError(type: Type) =
        AnswerableMisuseException(
            "A generator for an array with component type `${pool.getOriginalClass(type).sourceName}' was requested, " +
                "but no generator for that type was found."
        )

    fun accept(pair: kotlin.Pair<GeneratorType, Gen<*>?>) = accept(pair.first, pair.second)

    fun accept(generatorType: GeneratorType, gen: Gen<*>?) {
        if (gen != null) {
            // kotlin fails to smart cast here even though it says the cast isn't needed
            @Suppress("USELESS_CAST")
            knownGenerators[generatorType] = lazy { gen as Gen<*> }
        }
    }

    private fun request(generatorType: GeneratorType) {
        if (generatorType.requested == null) {
            request(generatorType.type)
        }
    }

    private fun request(type: Type) {
        when (type) {
            is Class<*> -> if (type.isArray) {
                request(type.componentType)
                knownGenerators[type.asGeneratorType()] =
                    lazy {
                        DefaultArrayGen(
                            knownGenerators[type.componentType.asGeneratorType()]?.value
                                ?: throw lazyArrayError(type.componentType),
                            type.componentType
                        )
                    }
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun generatorCompatible(requested: Type, known: Type): Boolean {
        // TODO: There are probably more cases we'd like to handle, but we should be careful to not be too liberal
        //  in matching
        if (requested == known) {
            return true
        }
        return when (requested) {
            is ParameterizedType -> when (known) {
                is ParameterizedType ->
                    requested.rawType == known.rawType &&
                        requested.actualTypeArguments.indices
                            .all {
                                generatorCompatible(
                                    requested.actualTypeArguments[it],
                                    known.actualTypeArguments[it]
                                )
                            }
                else -> false
            }
            is WildcardType -> when (known) {
                is Class<*> ->
                    requested.lowerBounds.elementAtOrNull(0) == known ||
                        requested.upperBounds.elementAtOrNull(0) == known
                is ParameterizedType -> {
                    val hasLower = requested.lowerBounds.size == 1
                    val matchesLower = hasLower && generatorCompatible(requested.lowerBounds[0], known)
                    val hasUpper = requested.upperBounds.size == 1
                    val matchesUpper = hasUpper && generatorCompatible(requested.upperBounds[0], known)
                    (!hasLower || matchesLower) && (!hasUpper || matchesUpper) && (hasLower || hasUpper)
                }
                else -> false
            }
            else -> false
        }
    }

    @Suppress("ReturnCount")
    private fun selectGenerator(goal: GeneratorType): Gen<*>? {
        // Selects a variant-compatible generator if an exact match isn't found
        // e.g. Kotlin Function1<? super Whatever, SomethingElse> (required) is compatible
        //        with Function1<        Whatever, SomethingElse> (known)
        knownGenerators[goal]?.value?.also { return it }
        knownGenerators.filter { (known, _) ->
            known.requested == goal.requested && generatorCompatible(goal.type, known.type)
        }.toList().firstOrNull()?.second?.also { return it.value }
        // As a final check before giving up the generator search, look on the class itself
        return if (goal.type !is Class<*> || goal.requested != null) {
            null
        } else {
            goal.klass.getAllGenerators().find {
                it.returnType == goal.type
            }?.let { generatorMethod ->
                CustomGen(generatorMethod).also { generator ->
                    accept(goal, generator)
                }
            }
        }
    }

    fun build(): GeneratorMap = requiredGenerators
        .map { it to (GenWrapper(selectGenerator(it) ?: throw lazyGenError(it.type), random)) }
        .toMap().toMutableMap()
        .also { discovered ->
            if (receiverType != null) {
                val receiverAsGeneratorType = receiverType.asGeneratorType()
                if (receiverAsGeneratorType !in discovered) {
                    knownGenerators[receiverAsGeneratorType]?.value?.also {
                        discovered[receiverAsGeneratorType] = GenWrapper(it, random)
                    }
                }
            }
        }.let {
            GeneratorMap(it)
        }
}

internal data class GeneratorMap(val map: Map<GeneratorType, GenWrapper<*>>) :
    Map<GeneratorType, GenWrapper<*>> by map {
    var parameterGenerator: GenWrapper<*>? = null

    @Suppress("NestedBlockDepth")
    fun generate(
        params: kotlin.Array<GeneratorType>,
        comp: Int
    ): kotlin.Array<Any?> {
        if (parameterGenerator != null) {
            return parameterGenerator!!.generate(comp).let {
                when (params.size) {
                    2 -> (it as Pair<*, *>).let {
                        arrayOf(it.first, it.second)
                    }
                    else -> error("Bad")
                }
            }
        }
        if (params.size > 1) {
            when (params.size) {
                2 -> this[GeneratorType(Pair::class.java)]
                else -> null
            }?.also {
                println("Here")
            }
        }
        return params.map { this[it]?.generate(comp) }.toTypedArray()
    }
}

internal class GenWrapper<T>(val gen: Gen<T>, private val random: Random) {
    operator fun invoke(complexity: Int) = gen.generate(complexity, random)

    fun generate(complexity: Int): T = gen.generate(complexity, random)
}

// So named as to avoid conflict with the @Generator annotation, as that class name is part of the public API
// and this one is not.
internal interface Gen<out T> {
    fun generate(complexity: Int, random: Random): T
}

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> Gen<T>.invoke(complexity: Int, random: Random): T = generate(complexity, random)
internal class CustomGen(private val gen: Method) :
    Gen<Any?> {
    override fun generate(complexity: Int, random: Random): Any? = gen(null, complexity, random)
}

internal class DefaultStringGen(private val cGen: Gen<*>) :
    Gen<String> {
    override fun generate(complexity: Int, random: Random): String {
        val len = random.nextInt(complexity + 1)

        return String((1..len).map { cGen(complexity, random) as Char }.toTypedArray().toCharArray())
    }
}

internal class DefaultArrayGen<T>(private val tGen: Gen<T>, private val tClass: Class<*>) :
    Gen<Any> {
    override fun generate(complexity: Int, random: Random): Any {
        return Array.newInstance(tClass, random.nextInt(complexity + 1)).also {
            val wrapper = ArrayWrapper(it)
            (0 until wrapper.size).forEach { idx -> wrapper[idx] = tGen(random.nextInt(complexity + 1), random) }
        }
    }
}

internal class DefaultListGen<T>(private val tGen: Gen<T>) :
    Gen<List<T>> {
    override fun generate(complexity: Int, random: Random): List<T> {
        fun genList(complexity: Int, length: Int): List<T> =
            if (length <= 0) {
                listOf()
            } else {
                listOf(tGen(random.nextInt(complexity + 1), random)) + genList(complexity, length - 1)
            }
        return genList(complexity, random.nextInt(complexity + 1))
    }
}

internal val defaultIntGen = object : Gen<Int> {
    override fun generate(complexity: Int, random: Random): Int {
        var comp = complexity
        if (complexity > Int.MAX_VALUE / 2) {
            comp = Int.MAX_VALUE / 2
        }
        return random.nextInt(comp * 2 + 1) - comp
    }
}
private const val MAX_FP_DENOMINATOR = (1e10 - 1)
internal val defaultDoubleGen = object : Gen<Double> {
    override fun generate(complexity: Int, random: Random): Double {
        val denom = random.nextDouble() * MAX_FP_DENOMINATOR + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return num / denom
    }
}
internal val defaultFloatGen = object : Gen<Float> {
    override fun generate(complexity: Int, random: Random): Float {
        return defaultDoubleGen(complexity, random).toFloat() // if complexity is > 1e38, this stops being uniform
    }
}
internal val defaultByteGen = object : Gen<Byte> {
    override fun generate(complexity: Int, random: Random): Byte {
        return (random.nextInt(complexity * 2 + 1) - complexity).toByte()
    }
}
internal val defaultShortGen = object : Gen<Short> {
    override fun generate(complexity: Int, random: Random): Short {
        return (random.nextInt(complexity * 2 + 1) - complexity).toShort()
    }
}
internal val defaultLongGen = object : Gen<Long> {
    // see Random.nextInt(int) algorithm.
    private fun Random.nextLong(bound: Long): Long {
        var bits: Long
        var value: Long
        do {
            bits = (nextLong() shl 1) shr 1
            value = bits % bound
        } while (bits - value + (bound - 1) < 0L)
        return value
    }

    override fun generate(complexity: Int, random: Random): Long {
        val complexityScaledToLargeValueRange = (complexity.toLong() * complexity.toLong() * 2).coerceAtLeast(0)
        return random.nextLong(complexityScaledToLargeValueRange * 2 + 1) - complexityScaledToLargeValueRange
    }
}
private const val FIRST_PRINTABLE_ASCII = 32
private const val LAST_PRINTABLE_ASCII = 126
private const val NUM_PRINTABLE_ASCII_CHARACTERS = LAST_PRINTABLE_ASCII - FIRST_PRINTABLE_ASCII + 1
private const val FIRST_ASTRAL_PLANE_UNICODE = 0x10000
private const val UNICODE_CHANCE = 0.15
private const val UNICODE_CHANCE_COMPLEXITY_SCALE = 32
internal val defaultCharGen = object : Gen<Char> {
    private fun Char.isPrintableAscii(): Boolean = this.toInt() in FIRST_PRINTABLE_ASCII..LAST_PRINTABLE_ASCII

    private fun Char.isPrint(): Boolean = isPrintableAscii() || Character.UnicodeBlock.of(this) in setOf(
        Character.UnicodeBlock.CYRILLIC,
        Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY,
        Character.UnicodeBlock.TAMIL,
        Character.UnicodeBlock.CURRENCY_SYMBOLS,
        Character.UnicodeBlock.ARROWS,
        Character.UnicodeBlock.SUPPLEMENTAL_ARROWS_A,
        Character.UnicodeBlock.ETHIOPIC_EXTENDED,
        Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT,
        Character.UnicodeBlock.KANGXI_RADICALS,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS,
        Character.UnicodeBlock.OLD_PERSIAN
    )

    override fun generate(complexity: Int, random: Random): Char {
        val complexityScaledUnicodeChance = complexity * UNICODE_CHANCE / UNICODE_CHANCE_COMPLEXITY_SCALE
        return if (random.nextDouble() < min(
            complexityScaledUnicodeChance,
            UNICODE_CHANCE
        )
        ) {
            var char: Char
            do {
                char = random.nextInt(FIRST_ASTRAL_PLANE_UNICODE).toChar()
            } while (!char.isPrint())
            char
        } else {
            (random.nextInt(NUM_PRINTABLE_ASCII_CHARACTERS) + FIRST_PRINTABLE_ASCII).toChar()
        }
    }
}
internal val defaultAsciiGen = object : Gen<Char> {
    override fun generate(complexity: Int, random: Random): Char {
        return (random.nextInt(NUM_PRINTABLE_ASCII_CHARACTERS) + FIRST_PRINTABLE_ASCII).toChar()
    }
}
internal val defaultBooleanGen = object : Gen<Boolean> {
    override fun generate(complexity: Int, random: Random): Boolean = random.nextBoolean()
}
internal val primitiveGenerators: Map<Class<*>, Gen<*>> = mapOf(
    Int::class.java to defaultIntGen,
    Double::class.java to defaultDoubleGen,
    Float::class.java to defaultFloatGen,
    Byte::class.java to defaultByteGen,
    Short::class.java to defaultShortGen,
    Long::class.java to defaultLongGen,
    Char::class.java to defaultCharGen,
    Boolean::class.java to defaultBooleanGen
)

data class GeneratorType(val type: Type, val requested: String? = null) {
    constructor(klass: Class<*>) : this(klass as Type)

    val klass by lazy { type as Class<*> }
}

fun Class<*>.asGeneratorType() = GeneratorType(this as Type, null)
