package com.empire.svc;

import com.empire.store.DatastoreClient;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntryServletTest {
  private EntryServlet servlet;
  private DatastoreClient dsClient;
  private LoginCache cache;
  private Request req;

  @Before
  public void setup() {
    dsClient = mock(DatastoreClient.class);
    cache = mock(LoginCache.class);
    req = mock(Request.class);
    servlet = new EntryServlet(dsClient, cache);
  }

  @Test
  public void getOrdersPasswordFailReturnsNull(){
    when(req.getPassword()).thenReturn(null);
  }
}
