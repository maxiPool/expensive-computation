package demo.reactor.expensivecomputation;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;


@SpringBootApplication
public class ExpensiveComputationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpensiveComputationApplication.class, args);
    }

}

@Slf4j
@RestController
@RequiredArgsConstructor
class MyController {

    private final StringLengthComputationManager stringLengthComputationManager;

    private final AtomicInteger nbRequests = new AtomicInteger(0);

    @GetMapping("{text}")
    public Integer getValue(@PathVariable("text") String text) {
        nbRequests.incrementAndGet();
        return stringLengthComputationManager.performExpensiveComputation(() -> theActualComputation(text), text);
//                .doOnNext(i -> System.out.printf("next result: %d%n", i));
    }

    @GetMapping("count")
    public Integer getRequestCount() {
        return nbRequests.get();
    }

    @SneakyThrows
    private Integer theActualComputation(String key) {
        log.info("START computing {}", key);
        Thread.sleep(4000);
        log.info("END   computing {}", key);
        return key.length();
    }

}

@Component
class StringLengthComputationManager extends AbstractExpensiveComputationManager<String, Integer> {
}

@Getter
@Slf4j
abstract class AbstractExpensiveComputationManager<S, T> {
    private final ConcurrentMap<S, Info<T>> computationMap = new ConcurrentHashMap<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Info<T> {
        private int subscriberCount;
        private CompletableFuture<T> value;
    }

    public T performExpensiveComputation(Supplier<T> expensiveComputation, S key) {
        var join = computationMap
                .compute(key, (k, v) -> ofNullable(v)
                        .map(i -> {
                            i.setSubscriberCount(i.getSubscriberCount() + 1);
                            return i;
                        })
                        .orElseGet(() -> new Info<T>(1, supplyAsync(expensiveComputation))))
                .getValue()
                .join();
        // how do I remove the entry from the Map safely?
        computationMap.computeIfPresent(key, (k, v) -> {
            if (v.getSubscriberCount() - 1 < 1) {
                return null;
            }
            v.setSubscriberCount(v.getSubscriberCount() - 1);
            return v;
        });
        return join;
    }

}
