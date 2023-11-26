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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


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
        return expensiveComputationManager.performExpensiveComputation(text);
//                .doOnNext(i -> System.out.printf("next result: %d%n", i));
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
        return Mono.just(getIntegerMono(key))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> computationMap.remove(key));
    }

    private static Integer getIntegerMono(String key) {
        // Replace this with your actual expensive computation logic
        simulateSlowAPICall(3000);
        return key.length();
    }

    public static void simulateSlowAPICall(int delayInMillis) {
        System.out.println("Simulating a slow API call...");
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Simulate a delay
            try {
                TimeUnit.MILLISECONDS.sleep(delayInMillis);
                System.out.println("Slow API call completed!");
            } catch (InterruptedException e) {
                System.out.println("Interrupted while simulating the API call");
                Thread.currentThread().interrupt();
            }
        });

        future.join(); // Wait for the CompletableFuture to complete
    }

}
