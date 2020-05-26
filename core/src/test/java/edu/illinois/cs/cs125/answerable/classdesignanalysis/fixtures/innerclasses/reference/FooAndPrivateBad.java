package edu.illinois.cs.cs125.answerable.classdesignanalysis.fixtures.innerclasses.reference;

/*
 * The fixture test for this reference vs the FooAndPrivate submission and vice versa
 * assert that extra/missing public classes are caught and that private classes aren't considered.
 */
public class FooAndPrivateBad {
    public class Foo {}
    public class Private {}
}
