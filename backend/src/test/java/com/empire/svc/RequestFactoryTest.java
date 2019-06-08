package com.empire.svc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RequestFactoryTest {
  private static RequestFactory factory;
  private static HttpServletRequest httpReq;

  private static final String unknownKey = "BOGUS_KEY";
  private static final String kingdomTest = "TEST_KINGDOM";
  private static final String passwordTest = "TEST_PW";

  @Before
  public void setup() {
    factory = new RequestFactory();
    httpReq = mock(HttpServletRequest.class);
  }

  private String embedQueryParam(String k, String v) {
    return "thing1=blah&" + k + "=" + v + "&thing2=blahblah";
  }

  private void mockInputStream(String s) {
    try {
      when(httpReq.getInputStream()).thenReturn(new TestServletInputStream(new ByteArrayInputStream(s.getBytes())));
    } catch(IOException e) {
      fail("IOException when mocking getInputStream()");
    }
  }

  private void mockInputStream() { mockInputStream(""); }

  @Test
  public void kingdomParsesSuccessfully () {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.kingdomKey, kingdomTest));
    mockInputStream();

    try {
      assertEquals(kingdomTest, factory.fromHttpServletRequest(httpReq).getKingdom());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void kingdomMissingParsesToDefault() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(unknownKey, kingdomTest));
    mockInputStream();

    try {
      assertNull(factory.fromHttpServletRequest(httpReq).getKingdom());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void passwordParsesSuccessfully () {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.passwordKey, passwordTest));
    mockInputStream();

    try {
      assertEquals(passwordTest, factory.fromHttpServletRequest(httpReq).getPassword());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void passwordMissingParsesToDefault() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(unknownKey, passwordTest));
    mockInputStream();

    try {
      assertNull(factory.fromHttpServletRequest(httpReq).getPassword());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  private class TestServletInputStream extends ServletInputStream {
    private final InputStream sourceStream;

    TestServletInputStream(InputStream sourceStream) { this.sourceStream = sourceStream; }
    @Override public int read() throws IOException { return this.sourceStream.read(); }
    @Override public boolean isFinished() { return false; }
    @Override public boolean isReady() { return true; }
    @Override public void setReadListener(ReadListener readListener) {}
    @Override public void close() throws IOException {
      super.close();
      this.sourceStream.close();
    }
  }
}
