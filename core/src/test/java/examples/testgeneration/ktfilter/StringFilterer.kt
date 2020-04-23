package examples.testgeneration.ktfilter

class StringFilterer(private val strings: List<CharSequence>) {

    fun filter(sieve: (CharSequence) -> Boolean): List<CharSequence> {
        return strings.filter(sieve)
    }
}
