package examples.testgeneration.standaloneverify.reference

import edu.illinois.cs.cs125.answerable.Generator
import edu.illinois.cs.cs125.answerable.Verify
import edu.illinois.cs.cs125.answerable.api.TestOutput
import java.util.Random
import org.junit.jupiter.api.Assertions

class KtStandaloneVerify(private val a: Int) {

    fun stringifyA(): String {
        return "$a"
    }

    fun squareA(): Int {
        return a * a
    }
}

@Generator
fun make(complexity: Int, random: Random): KtStandaloneVerify {
    return KtStandaloneVerify(random.nextInt(complexity + 1))
}

@Verify(standalone = true)
fun verify(ours: TestOutput<KtStandaloneVerify>, theirs: TestOutput<KtStandaloneVerify>) {
    Assertions.assertEquals(ours.receiver!!.stringifyA(), theirs.receiver!!.stringifyA())
    Assertions.assertEquals(ours.receiver!!.squareA(), theirs.receiver!!.squareA())
}
