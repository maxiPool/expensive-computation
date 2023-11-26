package demo.reactor.expensivecomputation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@SpringBootTest
class MyControllerTest {

    @Autowired
    private MyController myController;

    @Autowired
    private StringLengthComputationManager manager;

    @Test
    void test() throws InterruptedException {
        var exe = Executors.newScheduledThreadPool(10);

        var responses = new AtomicInteger();

        var nbOfRequestsPerKey = 200;
        for (int i = 0; i < nbOfRequestsPerKey; i++) {
            exe.schedule(() -> {
                        Integer allo = myController.getValue("allo");
                        responses.incrementAndGet();
//                                .subscribe(j -> responses.incrementAndGet())
                    },
                    i,
                    MILLISECONDS);
//            exe.schedule(() -> myController.getValue("bonjour")
//                            .subscribe(j -> responses.incrementAndGet()),
//                    i + nbOfRequestsPerKey,
//                    MILLISECONDS);
        }
        System.out.println("Done scheduling requests");
        Thread.sleep(5_000);

        System.out.printf("Computation map: %s%n", manager.getComputationMap().keySet());
        var soft = new SoftAssertions();
        soft.assertThat(manager.getComputationMap().keySet()).isEmpty();
        soft.assertThat(responses.get()).isEqualTo(nbOfRequestsPerKey);
        soft.assertAll();
        exe.shutdown();
    }

}
