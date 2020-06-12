package edu.illinois.cs.cs125.answerable

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
    val testGenerator = TestGenerator(YourBinaryTree::class.java, "size", TestRunnerArgs(numTests = 259, maxComplexity = 1024))
    repeat(repeats) {
        testGenerator.loadSubmission(examples.binarytree.size.YourBinaryTree::class.java)
            .runTests(0x0403, unsecuredEnvironment)
            .assertAllSucceeded()
    }
}

fun testClassic(repeats: Int) {
    repeat(repeats) {
        ClassicBinaryTreeSizeTest.testYourBinaryTreeSize()
    }
}

fun testSortingAnswerable(repeats: Int) {
    val testGenerator = TestGenerator(
        ArraySorter::class.java, "sort",
        TestRunnerArgs(maxComplexity = 1024, numSimpleEdgeMixedTests = 0, maxOnlySimpleCaseTests = 0, maxOnlyEdgeCaseTests = 0, numAllGeneratedTests = 1024)
    )
    repeat(repeats) {
        testGenerator.loadSubmission(examples.sorting.ArraySorter::class.java)
            .runTests(0x0403, unsecuredEnvironment)
            .assertAllSucceeded()
    }
}

fun testSortingClassic(repeats: Int) {
    repeat(repeats) {
        ClassicSortTest.test()
    }
}
