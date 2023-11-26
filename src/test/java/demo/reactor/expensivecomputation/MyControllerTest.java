package demo.reactor.expensivecomputation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Executors;

@SpringBootTest
class MyControllerTest {

    @Autowired
    private MyController myController;

    @Autowired
    private ExpensiveComputationManager manager;

    @Test
    void test() throws InterruptedException {
        var exe = Executors.newFixedThreadPool(6);

        for (int i = 0; i < 5; i++) {
            exe.submit(() -> myController.getValue("allo").subscribe());
            exe.submit(() -> myController.getValue("bonjour").subscribe());
        }
        myController.getValue("allo").block();
        myController.getValue("bonjour").block();

        System.out.printf("Computation map: %s%n", manager.getComputationMap().keySet());

        exe.shutdown();
    }

}
