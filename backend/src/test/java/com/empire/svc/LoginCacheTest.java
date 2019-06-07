package com.empire.svc;

import com.empire.store.DatastoreClient;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

  @Test
  public void checkLoginGetsCachedLogins() {
    IntStream.rangeClosed(0, 2).forEach(n -> cache.recordLogin(emailTest + n, gameIdTest, dateTest));

    List<String> emails = IntStream.rangeClosed(0, 3)
        .mapToObj(n -> emailTest + n)
        .peek(email -> when(dsClient.getLogin(email, gameIdTest, dateTest)).thenReturn(Optional.empty()))
        .collect(Collectors.toList());

    assertThat(cache.checkLogin(emails, gameIdTest, dateTest), Matchers.contains(true, true, true, false));
  }

  @Test
  public void checkLoginChecksCacheBeforeDatastore() {
    List<String> emails = IntStream.rangeClosed(0, 2)
        .mapToObj(n -> emailTest + n)
        .peek(email -> cache.recordLogin(email, gameIdTest, dateTest))
        .peek(email -> when(dsClient.getLogin(email, gameIdTest, dateTest)).thenReturn(Optional.empty()))
        .collect(Collectors.toList());

    assertThat(cache.checkLogin(emails, gameIdTest, dateTest), Matchers.contains(true, true, true));
    emails.forEach(email -> verify(dsClient, never()).getLogin(email, gameIdTest, dateTest));
  }

  @Test
  public void checkLoginFallsBackToDatastoreWhenNotInCache() {
    List<String> emails = Arrays.asList(emailTest + 0, emailTest + 1);

    cache.recordLogin(emailTest + 0, gameIdTest, dateTest);
    when(dsClient.getLogin(emailTest + 1, gameIdTest, dateTest)).thenReturn(Optional.of(LoginKey.create(emailTest + 1, gameIdTest, dateTest)));

    assertThat(cache.getRecordedKeys(), Matchers.containsInAnyOrder(LoginKey.create(emailTest + 0, gameIdTest, dateTest)));
    assertThat(cache.checkLogin(emails, gameIdTest, dateTest), Matchers.contains(true, true));
    verify(dsClient, never()).getLogin(emailTest + 0, gameIdTest, dateTest);
    verify(dsClient).getLogin(emailTest + 1, gameIdTest, dateTest);
  }

  @Test
  public void checkLoginReturnsFalseWhenKeyNotInCacheOrDatastore() {
    List<String> emails = IntStream.rangeClosed(0, 2)
        .mapToObj(n -> emailTest + n)
        .peek(email -> when(dsClient.getLogin(email, gameIdTest, dateTest)).thenReturn(Optional.empty()))
        .collect(Collectors.toList());

    assertThat(cache.getRecordedKeys(), Matchers.hasSize(0));
    assertThat(cache.checkLogin(emails, gameIdTest, dateTest), Matchers.contains(false, false, false));
    emails.forEach(email -> verify(dsClient).getLogin(email, gameIdTest, dateTest));
  }

  @Test
  public void fetchLoginHistoryReturnsCompleteForAllEmails() {
    List<String> emails = Arrays.asList(emailTest + 0, emailTest + 1);
    IntStream.rangeClosed(1, 2).forEach(n -> emails.forEach(email -> cache.recordLogin(email, gameIdTest, n)));
    emails.forEach(email -> when(dsClient.getLogin(email, gameIdTest, 3)).thenReturn(Optional.empty()));

    List<List<Boolean>> result = cache.fetchLoginHistory(emails, gameIdTest, 3);
    assertThat(result, Matchers.hasSize(3));
    assertThat(result.get(0), Matchers.contains(true, true));
    assertThat(result.get(1), Matchers.contains(true, true));
    assertThat(result.get(2), Matchers.contains(false, false));
  }
}
