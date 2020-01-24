package examples.ktaverage

class Average {

    fun average(numbers: DoubleArray): Double {
        var sum = 0.0
        numbers.forEach { sum += it }
        return sum / numbers.size
    }

}