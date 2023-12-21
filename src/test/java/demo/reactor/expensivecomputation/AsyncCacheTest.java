package demo.reactor.expensivecomputation;

import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.*;

class AsyncCacheTest {

  private StringLengthAsyncCache asyncCache;

  @BeforeEach
  void beforeEach() {
    asyncCache = new StringLengthAsyncCache();
  }

  @Test
  void should_computeOnceForManyComputationRequests() {
    try (var exe = Executors.newScheduledThreadPool(10)) {
      var responses = new AtomicInteger();

      for (int i = 0; i < 200; i++) {
        exe.schedule(() -> {
          asyncCache.compute("hello", () -> {
            sleepSneaky(500);
            responses.incrementAndGet();
            return "hello".length();
          });
        }, i, MILLISECONDS);
      }
      sleepSneaky(600);

      var soft = new SoftAssertions();
      soft.assertThat(responses.get()).isEqualTo(1);
      soft.assertAll();
    }
  }

  @Test
  void should_evictCache_whenExceptionThrownBySupplierAndAllSubscribersAreDone() {
    var key = "it will maybe fail";
    assertThatThrownBy(
        () -> asyncCache.compute(key, AsyncCacheTest::computationThatWillFail))
        .isInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(RuntimeException.class);
    assertThatNoException().isThrownBy(
        () -> asyncCache.compute(key, key::length));
  }

  @Test
  void should_useSameExceptionForAllSubscribersThatAreWaiting() {
    var key = "should_useSameExceptionForAllSubscribersThatAreWaiting";

    // noinspection unchecked
    var computationMap = (ConcurrentMap<String, CompletableFuture<Integer>>) ReflectionTestUtils.getField(asyncCache, "computationMap");
    assertThat(computationMap).isNotNull();

    var exception = new IllegalStateException("Something wrong!");
    computationMap.put(key, supplyAsync(() -> {
      throw exception;
    }));

    assertThatThrownBy(
        () -> asyncCache.compute(key, () -> {
          throw new NullPointerException("Shouldn't be here!");
        }))
        .rootCause()
        .isSameAs(exception)
        .isNotInstanceOf(NullPointerException.class)
        .hasMessageNotContaining("Shouldn't be here!");
  }

  private static Integer computationThatWillFail() {
    sleepSneaky(200);
    throw new IllegalStateException("Something wrong!");
  }

  @SneakyThrows
  private static void sleepSneaky(int ms) {
    sleep(ms);
  }

}
