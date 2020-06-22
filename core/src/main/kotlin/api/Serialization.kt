@file: Suppress("TooManyFunctions", "MatchingDeclarationName")
@file: JvmName("Serialization")
package edu.illinois.cs.cs125.answerable.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import edu.illinois.cs.cs125.answerable.DiscardedTestStep
import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.TestStep
import edu.illinois.cs.cs125.answerable.TestType
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAResult
import edu.illinois.cs.cs125.answerable.classdesignanalysis.OssifiedExecutable
import edu.illinois.cs.cs125.answerable.classdesignanalysis.OssifiedField
import kotlin.UnsupportedOperationException

internal val serializer: Moshi = Moshi.Builder()
    .add(OssifiedFieldAdapter())
    .add(OssifiedExecutableAdapter())
    .add(ExecutedTestStepAdapter())
    .add(DiscardedTestStepAdapter())
    .add(
        PolymorphicJsonAdapterFactory
            .of(TestStep::class.java, "stepKind")
            .withSubtype(ExecutedTestStep::class.java, "executed")
            .withSubtype(DiscardedTestStep::class.java, "discarded")
    )
    .add(TestingResultsAdapter())
    .add(KotlinJsonAdapterFactory())
    .build()

/** Quickly serialize an object whose runtime type that doesn't cause Moshi to vomit.
 * That should be pretty much anything that isn't parametric. Notably, lists and other sorts
 * of collections (except maybe sets and maps) won't work.
 *
 * You probably don't need to use this, but it may be useful for testing.
 */
internal fun unsafeSerialize(obj: Any): String {
    @Suppress("UNCHECKED_CAST")
    val adapter: JsonAdapter<Any> = serializer.adapter(obj::class.java) as JsonAdapter<Any>
    return adapter.toJson(obj)
}

fun CDAResult.toJson(): String =
    serializer.adapter(CDAResult::class.java).toJson(this)

fun TestingResults.toJson(): String =
    serializer.adapter(TestingResults::class.java).toJson(this)

/*
 * ADAPTERS:
 *
 * Moshi needs to be able to do all sorts of implicit reflection shenanigans with fields and parameters that
 * isn't possible for some of our objects. For these, we must provide custom adapters.
 *
 * Unfortunately, in many cases, the default _serialization_ would be fine, and Moshi's issue is with
 * deserialization. As far as I can tell, there's no way to use Moshi's serializer but change the deserializer :(
 *
 * Alternatives involve playing with the constructors of the OssifiedX types, and either
 * adding lots of out-of-place code or putting default values in the constructors that throw exceptions.
 * Neither is acceptable.
 */

internal class OssifiedFieldAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") unused: String): OssifiedField =
        throw UnsupportedOperationException("Can't deserialize OssifiedFields")

    class SerializableOssifiedField(
        val modifiers: List<String>,
        val type: String,
        val name: String,
        @Suppress("unused") val answerableName: String
    ) {
        internal constructor(ofield: OssifiedField) : this (
            ofield.modifiers,
            ofield.type,
            ofield.name,
            ofield.answerableName
        )
    }
    @ToJson
    fun toJson(ofield: OssifiedField): SerializableOssifiedField =
        SerializableOssifiedField(ofield)
}

internal class OssifiedExecutableAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") unused: String): OssifiedExecutable =
        throw UnsupportedOperationException("Can't deserialize OssifiedExecutables")

    @Suppress("unused", "LongParameterList")
    class SerializableOssifiedExecutable(
        val isDefault: Boolean,
        val modifiers: List<String>,
        val typeParams: List<String>,
        val returnType: String?,
        val name: String,
        val parameters: List<String>,
        val throws: List<String>,
        val answerableName: String
    ) {
        internal constructor(ossifiedExecutable: OssifiedExecutable) : this (
            ossifiedExecutable.isDefault,
            ossifiedExecutable.modifiers,
            ossifiedExecutable.typeParams,
            ossifiedExecutable.returnType,
            ossifiedExecutable.name,
            ossifiedExecutable.parameters,
            ossifiedExecutable.throws,
            ossifiedExecutable.answerableName
        )
    }
    @ToJson
    fun toJson(ossifiedExecutable: OssifiedExecutable): SerializableOssifiedExecutable =
        SerializableOssifiedExecutable(ossifiedExecutable)
}

internal class ExecutedTestStepAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") unused: String): ExecutedTestStep =
        throw UnsupportedOperationException("Can't deserialize ExecutedTestSteps")

    @Suppress("unused", "LongParameterList")
    class SerializableExecutedTestStep(
        val testNumber: Int,
        val wasDiscarded: Boolean,
        val testType: TestType,
        val refReceiver: OssifiedValue?,
        val subReceiver: OssifiedValue?,
        val succeeded: Boolean,
        val refOutput: OssifiedTestOutput,
        val subOutput: OssifiedTestOutput,
        val assertErr: String?
    ) {
        internal constructor(testStep: ExecutedTestStep) : this(
            testStep.testNumber,
            testStep.wasDiscarded,
            testStep.testType,
            testStep.refReceiver,
            testStep.subReceiver,
            testStep.succeeded,
            testStep.refOutput,
            testStep.subOutput,
            testStep.assertErr?.message
        )
    }

    @ToJson
    fun toJson(testStep: ExecutedTestStep): SerializableExecutedTestStep =
        SerializableExecutedTestStep(testStep)
}

internal class DiscardedTestStepAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") unused: String): DiscardedTestStep =
        throw UnsupportedOperationException("Can't deserialize DiscardTestSteps")

    @Suppress("unused")
    class SerializableDTS(val receiver: OssifiedValue?, val args: Array<OssifiedValue?>) {
        constructor(dts: DiscardedTestStep) : this(dts.ossifiedReceiver, dts.ossifiedArgs)
    }

    @ToJson
    fun toJson(dts: DiscardedTestStep): SerializableDTS = SerializableDTS(dts)
}

internal class TestingResultsAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") unused: String): TestingResults =
        throw UnsupportedOperationException("Can't deserialize TestingResults")

    @Suppress("unused", "LongParameterList")
    class SerializableTestingResults(
        val seed: Long,
        val testRunnerArgs: TestRunnerArgs,
        val solutionName: String,
        val startTime: Long,
        val endTime: Long,
        val timedOut: Boolean,
        val numDiscardedTests: Int,
        val numTests: Int,
        val numEdgeCaseTests: Int,
        val numSimpleCaseTests: Int,
        val numSimpleAndEdgeCaseTests: Int,
        val numMixedTests: Int,
        val numAllGeneratedTests: Int,
        val classDesignAnalysisResult: CDAResult,
        val testSteps: List<TestStep>
    ) {
        // Oh how I wish Kotlin had RecordPuns
        constructor(testingResults: TestingResults) : this(
            testingResults.seed,
            testingResults.testRunnerArgs,
            testingResults.solutionName,
            testingResults.startTime,
            testingResults.endTime,
            testingResults.timedOut,
            testingResults.numDiscardedTests,
            testingResults.numTests,
            testingResults.numEdgeCaseTests,
            testingResults.numSimpleCaseTests,
            testingResults.numSimpleAndEdgeCaseTests,
            testingResults.numMixedTests,
            testingResults.numAllGeneratedTests,
            testingResults.classDesignAnalysisResult,
            testingResults.testSteps
        )
    }

    @ToJson
    fun toJson(testingResults: TestingResults): SerializableTestingResults =
        SerializableTestingResults(testingResults)
}
