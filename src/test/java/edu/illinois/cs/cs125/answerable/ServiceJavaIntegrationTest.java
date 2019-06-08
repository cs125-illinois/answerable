package edu.illinois.cs.cs125.answerable;

import org.junit.jupiter.api.Test;

public class ServiceJavaIntegrationTest {

    @Test
    void testService() {
        Answerable answerable = new Answerable();

        answerable.loadNewQuestion("MyGreatNewQuestion", examples.lastten.correct.reference.LastTen.class);

        System.out.println(DefaultFrontend.toJson(
                answerable.submitAndTest("MyGreatNewQuestion", examples.lastten.correct.LastTen.class)
        ));
    }

}
