package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.api.OssifiedTestOutput
import edu.illinois.cs.cs125.answerable.api.OssifiedValue
import edu.illinois.cs.cs125.answerable.api.TestOutput
import java.lang.AssertionError

// TODO: serialize
data class TestStepV2(
    val testNumber: Int,
    val testKind: TestKind,
    val referenceReceiver: OssifiedValue?,
    val submissionReceiver: OssifiedValue?,
    val referenceLiveReceiver: Any?,
    val submissionDangerousLiveReceiver: Any?,
    val referenceOutput: OssifiedTestOutput,
    val submissionOutput: OssifiedTestOutput,
    val referenceLiveOutput: TestOutput<Any?>,
    val submissionDangerousLiveOutput: TestOutput<Any?>,

    val succeeded: Boolean,
    val assertionError: AssertionError?
)