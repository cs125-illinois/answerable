package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.LanguageMode
import edu.illinois.cs.cs125.answerable.annotations.Generator
import edu.illinois.cs.cs125.answerable.annotations.getAllGenerators
import edu.illinois.cs.cs125.answerable.annotations.isControlMethod
import edu.illinois.cs.cs125.answerable.annotations.usableName
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.answerableParams
import edu.illinois.cs.cs125.answerable.languageMode
import edu.illinois.cs.cs125.answerable.publicMethods
import edu.illinois.cs.cs125.answerable.simpleSourceName
import edu.illinois.cs.cs125.answerable.sourceName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.Random

internal data class GeneratorMapV2(
    val map: Map<GeneratorRequest, Gen<*>>,
    val receiverGenerator: ReceiverGenerator?
) : Map<GeneratorRequest, Gen<*>> by map {
    fun canGenerate(types: Array<GeneratorRequest>): Boolean = types.all { containsKey(it) }

    fun generateParams(types: Array<GeneratorRequest>, complexity: Int, random: Random): Array<Any?> {
        if (!canGenerate(types)) {
            throw AnswerableMisuseException(
                "Can't satisfy generator requests: ${types.filter { !containsKey(it) }}"
            )
        }
        return types.map { this[it]?.generate(complexity, random) }.toTypedArray()
    }

    fun generateReceiver(previous: Any?, iteration: Int, complexity: Int, random: Random): Any? =
        receiverGenerator?.generate(previous, iteration, complexity, random)
}

/**
 * Gets all generators related to the given class, as a GeneratorMapV2.
 *
 * A generator for G is "related" to a class C if:
 *   (1) It generates C, or
 *   (2) C exposes a public, non-Answerable method which has a parameter of type G.
 *
 * Generating receivers is different from generating parameters. Receivers can be generated via other strategies,
 * such as @Next methods or default constructors. The returned GeneratorMapV2 has a separate function,
 * [GeneratorMapV2.generateReceiver], for generating receiver objects of type [this].
 */
internal fun Class<*>.allRelatedGenerators(
    pool: TypePool = TypePool(),
    controlClass: Class<*>? = null
): GeneratorMapV2 {
    val testedMethods = this.publicMethods { !it.isControlMethod() }
    val paramRequests = testedMethods.flatMap { it.answerableParams.toSet() }.toSet()
    val languageMode = this.languageMode()

    val builder = GeneratorMapBuilderV2(paramRequests.toTypedArray(), this, pool, languageMode)
    val usableControlClass = controlClass ?: languageMode.findControlClass(this, pool) ?: this
    usableControlClass.getAllGenerators().forEach { method ->
        val annotation = method.getAnnotation(Generator::class.java)
        builder.addKnownGenerator(GeneratorRequest(method.genericReturnType, annotation.usableName), CustomGen(method))
    }

    return builder.build()
}

// [NOTE: Generator Keys]
// goalTypes holds types that we need generators for. @UseGenerator annotations allow specifying a specific generator.
// The name in the GeneratorRequest is non-null iff a specific generator is requested.

// [NOTE: Lazy closures in knownGenerators]
// In several cases, we need to construct parameterized generators. Eventually, this behavior will even be exposed
// for users to write parametric generators. Parametric generators require access to a generator for the actual
// type parameter; for example, an array generator needs access to a component generator. This are passed as arguments.
// Consider if the component is Int. We need to be sure we use the same Int generator that is being used for testing.
// To achieve this, the Gen<*>s held in the knownGenerators map are lazy. When the lazy closure is forced, by build(),
// it will grab the actual generator for the component type.
// In the case of default generators, lazy closures are also used to throw an error if there are duplicate external defaults.
//
// Even after parametric generation becomes exposed, arrays will have to be special-cased, because the array generators
// require access to a Class<*> instance so that they can use java.lang.reflect.Array.newInstance.
internal class GeneratorMapBuilderV2(
    goalTypes: Array<GeneratorRequest>,
    private val receiverClass: Class<*>?,
    private val pool: TypePool,
    languageMode: LanguageMode
) {
    private val requiredGenerators: Set<GeneratorRequest> = goalTypes.toSet()
    private val knownGenerators: MutableMap<GeneratorRequest, Lazy<Gen<*>>> = mutableMapOf()
    private val defaultGenerators: MutableMap<GeneratorRequest, Lazy<Gen<*>>> = mutableMapOf()
    private val duplicateGenErrors: MutableSet<String> = mutableSetOf()

    init {
        // request array types (has to be after defaultGenerator initialization)
        requiredGenerators.forEach(this::requestDefaultArrayGenerator)

        // add primitive types
        languageMode.defaultGenerators.forEach { addDefault(it.key.asGeneratorRequest(), it.value) }

        // add String default TODO: move into languageMode
        addDefault(
            String::class.java.asGeneratorRequest(),
            lazy {
                DefaultStringGen(
                    knownGenerators[Char::class.java.asGeneratorRequest()]!!.value
                )
            }
        )

        // search goal type classes for external defaults and add them
        goalTypes.mapNotNull { request ->
            findExternalDefaultGenerator(request)?.let { Pair(request, it) }
        }.forEach(::addDefault)
    }

    /**
     * Search a class for a default generator that generates instances of that class.
     *
     * When the generator is forced, an error may be thrown if there were duplicates.
     * This error will _not_ be thrown if the default is not needed.
     * If the generator is forced, it will be forced in [build].
     */
    private fun findExternalDefaultGenerator(request: GeneratorRequest): Lazy<Gen<*>>? {
        if (request.type is Class<*> && request.name == null) {
            val generatorMethods = request.type.getAllGenerators()
            val requestGenerators = generatorMethods.filter { it.returnType == request.type }
                // TODO: here we impose a restriction that external default generators are unnamed. But why?
                .filter { it.getAnnotation(Generator::class.java)?.name == "" }
            if (requestGenerators.size > 1) {
                return lazy {
                    throw AnswerableMisuseException(
                        "Multiple external default generators found on library class `${request.type.simpleSourceName}'"
                    )
                }
            }

            val generatorMethod = requestGenerators.getOrNull(0) ?: return null
            return lazy { CustomGen(generatorMethod) }
        }
        return null
    }

    /**
     * Merges [defaultGenerators] and [knownGenerators].
     *
     * If a request has a default generator, but not a known generator, the default is inserted as a known generator.
     * Otherwise, the default is ignored.
     */
    private fun fillInDefaults() {
        defaultGenerators.forEach { (request, gen) ->
            if (!knownGenerators.containsKey(request)) {
                knownGenerators[request] = gen
            }
        }
    }

    private fun duplicateGenError(request: GeneratorRequest) {
        val isDefaultGen = request.name == ""

        val msg = if (isDefaultGen) {
            "Multiple default generators found for type `${request.type}'"
        } else {
            "Multiple generators with name `${request.name}' found for type `${request.type}'"
        }

        duplicateGenErrors.add(msg)
    }

    private fun lazyGenError(request: GeneratorRequest) =
        AnswerableMisuseException(
            "No generator found for required generator `$request'"
        )


    private fun lazyArrayError(type: Type) =
        AnswerableMisuseException(
            "A generator for an array with component type `${pool.getOriginalClass(type).sourceName}' was requested, " +
                "but no generator for that component type was found."
        )

    /**
     * Add a "known generator;" a default-overriding generator that can be used to fulfil a request.
     * If a known generator already exists for this request, a duplicate generator error will be recorded,
     * and will be thrown when [build] is called.
     */
    fun addKnownGenerator(pair: Pair<GeneratorRequest, Gen<*>>) = addKnownGenerator(pair.first, pair.second)
    fun addKnownGenerator(generatorRequest: GeneratorRequest, gen: Gen<*>) {
        if (knownGenerators.containsKey(generatorRequest)) {
            duplicateGenError(generatorRequest)
        }
        knownGenerators[generatorRequest] = lazy { gen }
    }

    /**
     * Add a "default generator;" a generator that can be used to fulfil a request in the event that no known generator
     * is available. Defaults are generally expected to be unnamed, though this is not required or enforced.
     *
     * When the map is built, defaults will be used if there is no known generator for a request.
     */
    fun addDefault(generatorRequest: GeneratorRequest, gen: Gen<*>) = addDefault(generatorRequest, lazy { gen })
    fun addDefault(pair: Pair<GeneratorRequest, Lazy<Gen<*>>>) = addDefault(pair.first, pair.second)
    fun addDefault(generatorRequest: GeneratorRequest, gen: Lazy<Gen<*>>) {
        defaultGenerators[generatorRequest] = gen
    }

    private fun requestDefaultArrayGenerator(generatorRequest: GeneratorRequest) {
        if (generatorRequest.name == null) {
            requestDefaultArrayGenerator(generatorRequest.type)
        }
    }

    private fun requestDefaultArrayGenerator(type: Type) {
        when (type) {
            is Class<*> -> if (type.isArray) {
                requestDefaultArrayGenerator(type.componentType)
                addDefault(type.asGeneratorRequest(),
                    // see [Note: Lazy closures in knownGenerators]
                    lazy {
                        DefaultArrayGen(
                            knownGenerators[type.componentType.asGeneratorRequest()]?.value
                                ?: throw lazyArrayError(type.componentType),
                            type.componentType
                        )
                    }
                )
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
    /**
     * Selects a known generator that matches the request and forces it.
     */
    private fun selectGenerator(goal: GeneratorRequest): Gen<*>? {
        // Selects a variant-compatible generator if an exact match isn't found
        // e.g. Kotlin Function1<? super Whatever, SomethingElse> (required) is compatible
        //        with Function1<        Whatever, SomethingElse> (known)
        knownGenerators[goal]?.value?.also { return it }
        knownGenerators.filter { (known, _) ->
            known.name == goal.name && generatorCompatible(goal.type, known.type)
        }.toList().firstOrNull()?.second?.also { return it.value }
        return null
    }

    fun build(): GeneratorMapV2 {
        // throw recorded errors
        if (duplicateGenErrors.isNotEmpty()) {
            throw AnswerableMisuseException(duplicateGenErrors.joinToString(separator = "\n"))
        }

        fillInDefaults()

        val selectedGenerators = requiredGenerators
            .map { it to (selectGenerator(it) ?: throw lazyGenError(it)) }
            .toMap()

        val receiverGenerator = if (receiverClass != null) {
            ReceiverGenerator.of(receiverClass, selectedGenerators)
        } else {
            null
        }

        return GeneratorMapV2(selectedGenerators, receiverGenerator)
    }
}