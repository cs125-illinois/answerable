@file:Suppress("unused", "UNUSED_PARAMETER", "FunctionOnlyReturningConstant", "EmptyFunctionBlock")

package edu.illinois.cs.cs125.answerable.annotations.fixtures

import edu.illinois.cs.cs125.answerable.annotations.Solution
import edu.illinois.cs.cs125.answerable.annotations.Verify
import edu.illinois.cs.cs125.answerable.api.TestOutput
import java.util.Random

@Verify(name = "0")
fun correct0(first: TestOutput<*>?, second: TestOutput<*>?) { }

@Verify(name = "1")
fun correct1(first: TestOutput<*>?, second: TestOutput<*>?, random: Random?) { }

@Verify(name = "2")
fun broken0(first: TestOutput<*>?) { }

@Verify(name = "3")
fun broken1(first: TestOutput<*>?, second: TestOutput<*>?): Int = 0

class TestValidateVerifyKt {
    @Solution(name = "0")
    fun solution0() { }

    @Solution(name = "1")
    fun solution1() { }

    @Solution(name = "2")
    fun solution2() { }

    @Solution(name = "3")
    fun solution3() { }
}
