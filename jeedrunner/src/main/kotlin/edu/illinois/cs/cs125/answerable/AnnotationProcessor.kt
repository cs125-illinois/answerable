package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.DefaultTestRunArguments
import edu.illinois.cs.cs125.answerable.api.Solution
import edu.illinois.cs.cs125.answerable.api.Timeout
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class AnswerableAnnotationOwner : AbstractProcessor() {

    private val answerableClassAnnotations
            = setOf(Solution::class, Timeout::class, DefaultTestRunArguments::class)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment?): Boolean {
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return answerableClassAnnotations.map { it.java.name }.toSet()
    }

}