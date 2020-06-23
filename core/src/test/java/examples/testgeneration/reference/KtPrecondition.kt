@file:Suppress("unused")

package examples.testgeneration.reference

import edu.illinois.cs.cs125.answerable.annotations.Precondition
import edu.illinois.cs.cs125.answerable.annotations.Solution

class KtPrecondition {
    @Solution
    fun firstLetter(text: String): Char {
        return if (text.isEmpty()) '?' else text.toCharArray()[0]
    }
}

@Precondition
fun precondition(text: String): Boolean {
    return text.isNotEmpty()
}
