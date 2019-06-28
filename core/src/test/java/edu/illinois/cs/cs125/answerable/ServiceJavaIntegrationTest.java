package edu.illinois.cs.cs125.answerable;

import edu.illinois.cs.cs125.answerable.api.Answerable;
import org.junit.jupiter.api.Test;

class ServiceJavaIntegrationTest {

    @Test
    void testService() {
        Answerable answerable = new Answerable(TestEnvironment.getUnsecuredEnvironment());

        answerable.loadNewQuestion("LastTen", examples.lastten.correct.reference.LastTen.class);

        System.out.println(answerable
                .submitAndTest("LastTen", examples.lastten.correct.LastTen.class).toJson()
        );
    }

}
