package edu.illinois.cs.cs125.answerable.core

import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

@Suppress("RemoveSingleExpressionStringTemplate")
class TestClassDesign : StringSpec({
    solution(examples.submissiondesign.Correct::class.java).also { solution ->
        "${solution.solution.testName()}" {
            solution.submission(examples.submissiondesign.Correct1::class.java)
            shouldThrow<ClassDesignError> {
                solution.submission(examples.submissiondesign.MissingMethod1::class.java)
            }
            shouldThrow<ClassDesignError> {
                solution.submission(examples.submissiondesign.MissingConstructor1::class.java)
            }
        }
    }
})
