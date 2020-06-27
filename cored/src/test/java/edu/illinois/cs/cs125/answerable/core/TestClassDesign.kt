import edu.illinois.cs.cs125.answerable.core.ClassDesignError
import edu.illinois.cs.cs125.answerable.core.answerable
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

@Suppress("RemoveSingleExpressionStringTemplate")
class TestClassDesign : StringSpec({
    examples.submissiondesign.Correct::class.java.also {
        "${it.testName()}" {
            it.answerable().check(examples.submissiondesign.Correct1::class.java)
            shouldThrow<ClassDesignError> {
                it.answerable().check(examples.submissiondesign.MissingMethod1::class.java)
            }
            shouldThrow<ClassDesignError> {
                it.answerable().check(examples.submissiondesign.MissingConstructor1::class.java)
            }
        }

    }
})

