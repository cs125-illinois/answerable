package examples

class Accumulator(private val initialValue: Int) {
    private var currentValue = initialValue
    fun add(more: Int) {
        currentValue += more
    }
    val current: Int
        get() = currentValue
}
