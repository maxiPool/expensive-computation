package demo.reactor.expensivecomputation;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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

@Slf4j
abstract class AbstractExpensiveComputationManager<S, T> {
  private final ConcurrentMap<S, ComputationSubscriber<T>> computationMap = new ConcurrentHashMap<>();

  public T compute(Supplier<T> expensiveComputation, S key) {
    return computationMap
        .compute(key, (k, v) -> ofNullable(v)
            .orElseGet(() -> new AsyncComputation<T>(expensiveComputation)))
        .getValue()
        .exceptionally(throwable -> {
          throw new RuntimeException(throwable);
        })
        .whenComplete((result, ex) -> computationMap.remove(key))
        .join();
  }

  private static class AsyncComputation<T> {
    @Getter
    private final CompletableFuture<T> value;

    AsyncComputation(Supplier<T> supplier) {
      this.value = supplyAsync(supplier);
    }
  }

}
