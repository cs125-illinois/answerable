package edu.illinois.cs.cs125.answerable.classdesignanalysis.fixtures.validate;

import edu.illinois.cs.cs125.answerable.EdgeCase;
import edu.illinois.cs.cs125.answerable.SimpleCase;

public class TestValidateCaseMethods {
    @SimpleCase
    public static int[] correct1() {
        return new int[] { };
    }
    @EdgeCase
    public static int[] correct2() {
        return new int[] { };
    }
    @SimpleCase
    public static String[] correct3() {
        return new String[] { };
    }
    @EdgeCase
    public static String[] correct4() {
        return new String[] { };
    }

    @SimpleCase
    @EdgeCase
    public static String[] broken1() {
        return new String[] { };
    }
    @SimpleCase
    public int[] broken2() {
        return new int[] { };
    }
    @SimpleCase
    public static int[] broken2(int unused) {
        return new int[] { };
    }
    @SimpleCase
    public static int broken4() {
        return 0;
    }
    @EdgeCase
    public String[] broken5() {
        return new String[] { };
    }
    @EdgeCase
    public static String[] broken6(int unused) {
        return new String[] { };
    }
    @EdgeCase
    public static String broken7() {
        return "";
    }
}
