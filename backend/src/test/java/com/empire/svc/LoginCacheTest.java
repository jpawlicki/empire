package com.empire.svc;

import com.empire.store.DatastoreClient;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class LoginCacheTest {
  private static DatastoreClient dsClient;
  private static LoginCache cache;

  private final String emailTest = "email@test.com";
  private final long gameIdTest = 3;
  private final int dateTest = 2;
  private final LoginKey loginKeyTest = LoginKey.create(emailTest, gameIdTest, dateTest);

  @Before
  public void setup() {
    dsClient = mock(DatastoreClient.class);
    cache = new LoginCache(dsClient);
  }

  @Test
  public void recordLoginSavesLoginKey() {
    cache.recordLogin(emailTest, gameIdTest, dateTest);

    verify(dsClient).putLogin(emailTest, gameIdTest, dateTest);
    assertThat(cache.getRecordedKeys(), Matchers.containsInAnyOrder(loginKeyTest));
  }

  @Test
  public void recordLoginSavesMultipleLoginKeys() {
    List<LoginKey> keys = IntStream.rangeClosed(0, 2)
        .peek(n -> cache.recordLogin(emailTest, gameIdTest, dateTest + n))
        .mapToObj(n -> LoginKey.create(emailTest, gameIdTest, dateTest + n))
        .collect(Collectors.toList());

    IntStream.rangeClosed(0, 2).forEach(n -> verify(dsClient).putLogin(emailTest, gameIdTest, dateTest + n));
    assertThat(cache.getRecordedKeys(), Matchers.containsInAnyOrder(keys.get(0), keys.get(1), keys.get(2)));
  }

  @Test
  public void clearMethodEmptiesCache() {
    IntStream.rangeClosed(0, 2).forEach(n -> cache.recordLogin(emailTest, gameIdTest, dateTest + n));
    assertThat(cache.getRecordedKeys(), Matchers.hasSize(3));

    cache.clear();
    assertThat(cache.getRecordedKeys(), Matchers.hasSize(0));
  }
}
