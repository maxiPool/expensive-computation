package demo.reactor.expensivecomputation;

import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MyControllerTest {

  @Autowired
  private MyController myController;

  @Autowired
  private StringLengthComputationManager manager;

  @Test
  void test() throws InterruptedException {
    try (var exe = Executors.newScheduledThreadPool(10)) {

      var responses = new AtomicInteger();

      var nbOfRequestsPerKey = 200;
      for (int i = 0; i < nbOfRequestsPerKey; i++) {
        exe.schedule(() -> {
          responses.incrementAndGet();
        }, i, MILLISECONDS);
      }
      sleep(5_000);

      var soft = new SoftAssertions();
      soft.assertThat(responses.get()).isEqualTo(nbOfRequestsPerKey);
      soft.assertAll();
    }
  }

  @Test
  void should_evictCache_whenExceptionThrownBySupplierAndAllSubscribersAreDone() {
    assertThatThrownBy(
        () -> manager.performExpensiveComputation(MyControllerTest::computationThatWillFail, "it will fail"))
        .isInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(RuntimeException.class);
    assertThatNoException().isThrownBy(
        () -> manager.performExpensiveComputation("it will fail"::length, "it will fail"));
  }

  @Test
  void should_useExceptionForAllSubscribersThatAreWaiting() {
    var assertOne = runAsync(() -> assertThatThrownBy(
        () -> manager.performExpensiveComputation(MyControllerTest::computationThatWillFail, "it will fail"))
        .isInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(RuntimeException.class));

    var assertTwo = assertOne
        .thenCompose(result -> runAsync(() -> {
          getSneakySleep(50); // Sleep for 0.1 second (100 milliseconds)
          assertThatThrownBy(
              () -> manager.performExpensiveComputation(MyControllerTest::computationThatWillFail, "it will fail"))
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(RuntimeException.class);
        }));

    allOf(assertOne, assertTwo);
  }

  private static Integer computationThatWillFail() {
    getSneakySleep(200);
    throw new IllegalStateException("Something wrong!");
  }

  @SneakyThrows
  private static void getSneakySleep(int ms) {
    sleep(ms);
  }

}
