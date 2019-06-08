package com.empire.svc;

import com.empire.store.DatastoreClient;
import org.junit.Before;
import static org.mockito.Mockito.mock;

public class EntryServletBackendTest {
  private static EntryServletBackend backend;
  private static DatastoreClient dsClient;
  private static PasswordValidator passVal;
  private static LoginCache cache;

  @Before
  public void setup() {
    dsClient = mock(DatastoreClient.class);
    passVal = mock(PasswordValidator.class);
    cache = mock(LoginCache.class);

    backend = new EntryServletBackend(dsClient, passVal, cache);
  }
}
