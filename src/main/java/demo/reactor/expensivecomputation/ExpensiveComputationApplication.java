package demo.reactor.expensivecomputation;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;


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
    public Integer getValue(@PathVariable("text") String text) {
        return expensiveComputationManager.performExpensiveComputation(text);
//                .doOnNext(i -> System.out.printf("next result: %d%n", i));
    }

}

@Service
@Getter
@Slf4j
class ExpensiveComputationManager {
    private final ConcurrentMap<String, Info> computationMap = new ConcurrentHashMap<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Info {
        private int subscriberCount;
        private CompletableFuture<Integer> value;
    }

    public Integer performExpensiveComputation(String key) {
        Integer join = computationMap
                .compute(key, (k, v) -> ofNullable(v)
                        .map(i -> {
                            i.setSubscriberCount(i.getSubscriberCount() + 1);
                            return i;
                        })
                        .orElseGet(() -> new Info(1, supplyAsync(() -> theActualComputation(key)))))
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

    @SneakyThrows
    private Integer theActualComputation(String key) {
        log.info("START computing");
        Thread.sleep(200);
        log.info("END   computing");
        return key.length();
    }

}
