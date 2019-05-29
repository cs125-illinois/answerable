package edu.illinois.cs.cs125.answerable

class SubmissionMismatchException(msg: String) : Exception("\n$msg")
class ClassDesignMismatchException(val msg: String) : Exception("\n$msg") // msg is a property for testing.