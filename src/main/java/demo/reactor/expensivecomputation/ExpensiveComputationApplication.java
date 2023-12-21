package demo.reactor.expensivecomputation;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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

  private final StringLengthAsyncCache manager = new StringLengthAsyncCache();

  private final AtomicInteger nbRequests = new AtomicInteger(0);

  @GetMapping("{text}")
  public Integer getValue(@PathVariable("text") String text) {
    nbRequests.incrementAndGet();
    return manager.compute(text, () -> theActualComputation(text));
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

class StringLengthAsyncCache extends AbstractAsyncCache<String, Integer> {
}

@Slf4j
abstract class AbstractAsyncCache<S, T> {
  private final ConcurrentMap<S, CompletableFuture<T>> computationMap = new ConcurrentHashMap<>();

  public T compute(S key, Supplier<T> expensiveComputation) {
    return computationMap
        .computeIfAbsent(key, k -> supplyAsync(expensiveComputation))
        .exceptionally(throwable -> {
          throw new RuntimeException(throwable);
        })
        .whenComplete((result, ex) -> computationMap.remove(key))
        .join();
  }
}
