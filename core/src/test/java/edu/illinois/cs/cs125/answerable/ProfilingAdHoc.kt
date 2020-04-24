package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.Service
import examples.binarytree.reference.YourBinaryTree
import examples.binarytree.size.ClassicBinaryTreeSizeTest
import examples.sorting.ClassicSortTest
import examples.sorting.reference.ArraySorter

fun main() {
    val testFunc = ::testSortingAnswerable
    testFunc(1)
    println("Press Enter to start")
    System.`in`.read()
    println("\nProceeding")
    val startTime = System.currentTimeMillis()
    testFunc(256)
    val endTime = System.currentTimeMillis()
    println("Took ${endTime - startTime} ms")
}

fun testAnswerable(repeats: Int) {
    val answerableService = Service(defaultEnvironment)
    answerableService.loadNewQuestion("size", YourBinaryTree::class.java, "size", TestRunnerArgs(numTests = 259, maxComplexity = 1024))
    repeat(repeats) {
        val result = answerableService.submitAndTest("size", examples.binarytree.size.YourBinaryTree::class.java)
        result.assertAllSucceeded()
    }
}

fun testClassic(repeats: Int) {
    repeat(repeats) {
        ClassicBinaryTreeSizeTest.testYourBinaryTreeSize()
    }
}

fun testSortingAnswerable(repeats: Int) {
    val answerableService = Service(defaultEnvironment)
    answerableService.loadNewQuestion("sort", ArraySorter::class.java,
            TestRunnerArgs(maxComplexity = 1024, numSimpleEdgeMixedTests = 0, maxOnlySimpleCaseTests = 0, maxOnlyEdgeCaseTests = 0, numAllGeneratedTests = 1024))
    repeat(repeats) {
        val result = answerableService.submitAndTest("sort", examples.sorting.ArraySorter::class.java)
        result.assertAllSucceeded()
    }
}

fun testSortingClassic(repeats: Int) {
    repeat(repeats) {
        ClassicSortTest.test()
    }
}
