package edu.illinois.cs.cs125.answerable.annotations.fixtures

import edu.illinois.cs.cs125.answerable.annotations.Next
import java.util.Random

class TestValidateNextKt

@Next
fun correct0(current: TestValidateNextKt?, iteration: Int, random: Random): TestValidateNextKt? = null

@Next
fun broken0(current: String?, iteration: Int, random: Random): TestValidateNextKt? = null

@Next
fun broken1(current: TestValidateNextKt?, iteration: Int, random: Random): String? = null
