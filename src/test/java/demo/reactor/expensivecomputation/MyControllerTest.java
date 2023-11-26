package demo.reactor.expensivecomputation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class MyControllerTest {

    @Autowired
    private MyController myController;

    @Autowired
    private ExpensiveComputationManager manager;

    @Test
    void test() throws InterruptedException {
        var exe = Executors.newVirtualThreadPerTaskExecutor();

        var bonjour = "bonjour";
        var allo = "allo";
        var responses = new AtomicInteger();

        var nbOfRequestsPerKey = 250;
        for (int i = 0; i < nbOfRequestsPerKey; i++) {
            exe.submit(() -> myController.getValue(allo)
                    .subscribe(j -> responses.incrementAndGet()));
            exe.submit(() -> myController.getValue(bonjour)
                    .subscribe(j -> responses.incrementAndGet()));
        }
//        allos.add(myController.getValue("allo").block());
//        bonjours.add(myController.getValue("bonjour").block());
        Thread.sleep(10_000);

        System.out.printf("Computation map: %s%n", manager.getComputationMap().keySet());
        var soft = new SoftAssertions();
        soft.assertThat(manager.getComputationMap().keySet()).isEmpty();
        soft.assertThat(responses.get()).isEqualTo(nbOfRequestsPerKey * 2);
        soft.assertAll();
        exe.shutdown();
    }

}
