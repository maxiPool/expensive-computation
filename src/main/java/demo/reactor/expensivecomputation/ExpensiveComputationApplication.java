package demo.reactor.expensivecomputation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@SpringBootApplication
public class ExpensiveComputationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpensiveComputationApplication.class, args);
    }

}

@RestController
@RequiredArgsConstructor
class MyController {

    private final ExpensiveComputationManager expensiveComputationManager;

    @GetMapping("{text}")
    public Mono<Integer> getValue(@PathVariable("text") String text) {
        return expensiveComputationManager.performExpensiveComputation(text)
                .doOnNext(i -> System.out.printf("next result: %d%n", i));
    }

}

@Service
@Getter
class ExpensiveComputationManager {
    private final ConcurrentMap<String, Mono<Integer>> computationMap = new ConcurrentHashMap<>();

    public Mono<Integer> performExpensiveComputation(String key) {
        return computationMap
                .computeIfAbsent(key, k -> theActualComputation(key));
    }

    private Mono<Integer> theActualComputation(String key) {
        // Replace this with your actual expensive computation logic
        try {
            System.out.printf("START computation on %s%n", key);
            Thread.sleep(3000); // Simulating computation time
            System.out.printf("END   computation on %s%n", key);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return Mono.just(key.length())
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> computationMap.remove(key));
    }
}
