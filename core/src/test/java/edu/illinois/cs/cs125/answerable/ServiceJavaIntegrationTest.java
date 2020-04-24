package edu.illinois.cs.cs125.answerable;

import edu.illinois.cs.cs125.answerable.api.Service;
import org.junit.jupiter.api.Test;

class ServiceJavaIntegrationTest {

    @Test
    void testService() {
        Service service = new Service(TestEnvironment.getUnsecuredEnvironment());

        service.loadNewQuestion("LastTen", examples.lastten.correct.reference.LastTen.class);

        System.out.println(service
                .submitAndTest("LastTen", examples.lastten.correct.LastTen.class).toJson()
        );
    }

}
