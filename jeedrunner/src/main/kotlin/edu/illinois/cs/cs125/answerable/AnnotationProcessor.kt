package edu.illinois.cs.cs125.answerable

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * An annotation processor that claims responsibility for all Answerable annotations but otherwise does nothing.
 * This is needed to suppress a warning during Jeed compilation about no processor claiming the annotations.
 */
class AnswerableAnnotationOwner : AbstractProcessor() {

    /** ALl Answerable annotations, from Annotations.kt in the main module. */
    private val answerableAnnotations = setOf(
            Solution::class,
            Timeout::class,
            Next::class,
            Generator::class,
            UseGenerator::class,
            EdgeCase::class,
            SimpleCase::class,
            Precondition::class,
            Verify::class,
            Helper::class,
            Ignore::class,
            DefaultTestRunArguments::class)

    override fun process(annotations: Set<TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        // Do nothing, say the annotations were handled
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return answerableAnnotations.map { it.java.name }.toSet()
    }

}