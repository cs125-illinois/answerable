package examples.ktfilter.reference

import edu.illinois.cs.cs125.answerable.api.Generator
import edu.illinois.cs.cs125.answerable.api.Solution
import edu.illinois.cs.cs125.answerable.api.defaultStringGenerator
import java.util.*

class StringFilterer(private val strings: List<CharSequence>) {

    @Solution
    fun filter(sieve: (CharSequence) -> Boolean): List<CharSequence> {
        val results = mutableListOf<CharSequence>()
        strings.forEach {
            if (sieve(it)) results.add(it)
        }
        return results
    }

    companion object {

        @Generator
        @JvmStatic
        fun makeSieve(complexity: Int, random: Random): (CharSequence) -> Boolean {
            return { true }
        }

        @Generator
        @JvmStatic
        fun makeFilterer(complexity: Int, random: Random): StringFilterer {
            return StringFilterer((0..complexity).map { defaultStringGenerator(complexity, random) }.toList())
        }

    }

}