package edu.illinois.cs.cs125.answerable.jeedrunner

import java.io.File

// TODO: Nicer command-line interface
@Suppress("MagicNumber")
fun main(args: Array<String>) {
    if (args.size == 2 || args.size == 3) {
        val refFname = args[0]
        val subFname = args[1]

        val comCM = if (args.size == 3) args[2] else null

        val refFile = File(refFname)
        val subFile = File(subFname)
        val comFile = comCM?.run { File(comCM) }

        val className = refFile.nameWithoutExtension

        println(testFromStrings(refFile.readText(), subFile.readText(), comFile?.readText(), className).toJson())
    } else {
        println("Please provide a reference.")
        val ref: String = readLine()!!
        println("Please provide a solution attempt")
        val sub: String = readLine()!!
        println("Is there any common code to provide? (y/n)")
        val resp: String = readLine()!!
        val common: String? = if (resp == "y" || resp == "Y") {
            readLine()!!
        } else {
            null
        }

        println(testFromStrings(ref, sub, common, "Test").toJson())
    }
}
