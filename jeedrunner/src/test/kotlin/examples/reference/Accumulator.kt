package examples.reference

import edu.illinois.cs.cs125.answerable.annotations.Generator
import edu.illinois.cs.cs125.answerable.annotations.Solution
import edu.illinois.cs.cs125.answerable.annotations.Verify
import edu.illinois.cs.cs125.answerable.api.TestOutput
import edu.illinois.cs.cs125.answerable.api.defaultIntGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.Random

class Accumulator(private var value: Int) {
    @Solution
    fun add(extra: Int) {
        value += extra
    }
    val current: Int
        get() = value
}

@Generator
fun generate(complexity: Int, random: Random): Accumulator {
    return Accumulator(defaultIntGenerator(complexity, random))
}

@Verify
fun verify(ours: TestOutput<Accumulator>, theirs: TestOutput<Accumulator>) {
    assertEquals(ours.receiver!!.current, theirs.receiver!!.current)
}
