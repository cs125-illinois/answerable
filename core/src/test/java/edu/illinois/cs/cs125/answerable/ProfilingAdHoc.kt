package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.Answerable
import examples.binarytree.reference.YourBinaryTree

fun main() {
    println("Press Enter to start")
    System.`in`.read()
    println("\nProceeding")
    val answerableService = Answerable(defaultEnvironment)
    repeat(8) { q ->
        answerableService.loadNewQuestion("size", YourBinaryTree::class.java, "size")
        repeat(24) { a ->
            val result = answerableService.submitAndTest("size", examples.binarytree.size.YourBinaryTree::class.java)
            result.assertAllSucceeded(showOutput = false)
            println("Finished $q $a")
        }
    }
}