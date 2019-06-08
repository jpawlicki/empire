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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RequestFactoryTest {
  private static RequestFactory factory;
  private static HttpServletRequest httpReq;

  private static final String kingdomTest = "TEST_KINGDOM";
  private static final String passwordTest = "TEST_PW";
  private static final int versionTest = 12;
  private static final int turnTest = 26;
  private static final long gameIdTest = 42;
  private static final String bodyTest = "TEST_BODY";

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
  public void missingParametersParseToDefaults() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam("unknownkey", "unknownval"));
    mockInputStream();

    try {
      Request defaultRequest = new Request(0, 0, -1, null, null, "", false);
      assertEquals(defaultRequest, factory.fromHttpServletRequest(httpReq));
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void nullQueryStringParsesToDefaults() {
    when(httpReq.getQueryString()).thenReturn(null);
    mockInputStream();

    try {
      Request defaultRequest = new Request(0, 0, -1, null, null, "", false);
      assertEquals(defaultRequest, factory.fromHttpServletRequest(httpReq));
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void kingdomParsesSuccessfully() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.kingdomKey, kingdomTest));
    mockInputStream();

    try {
      assertEquals(kingdomTest, factory.fromHttpServletRequest(httpReq).getKingdom());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void passwordParsesSuccessfully() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.passwordKey, passwordTest));
    mockInputStream();

    try {
      assertEquals(passwordTest, factory.fromHttpServletRequest(httpReq).getPassword());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void versionParsesSuccessfully() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.versionKey, Integer.toString(versionTest)));
    mockInputStream();

    try {
      assertEquals(versionTest, factory.fromHttpServletRequest(httpReq).getVersion());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void turnParsesSuccessfully() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.turnKey, Integer.toString(turnTest)));
    mockInputStream();

    try {
      assertEquals(turnTest, factory.fromHttpServletRequest(httpReq).getTurn());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void gameIdParsesSuccessfully() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.gameIdKey, Long.toString(gameIdTest)));
    mockInputStream();

    try {
      assertEquals(gameIdTest, factory.fromHttpServletRequest(httpReq).getGameId());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void skipmailParsesSuccessfullyTrue() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.skipmailKey, RequestFactory.TRUE));
    mockInputStream();

    try {
      assertTrue(factory.fromHttpServletRequest(httpReq).skipmail());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void skipmailParsesSuccessfullyFalse() {
    when(httpReq.getQueryString()).thenReturn(embedQueryParam(RequestFactory.skipmailKey, RequestFactory.FALSE));
    mockInputStream();

    try {
      assertFalse(factory.fromHttpServletRequest(httpReq).skipmail());
    } catch (IOException e) {
      fail("IOException during test");
    }
  }

  @Test
  public void bodyParsesSuccessfully() {
    mockInputStream(bodyTest);

    try {
      assertEquals(bodyTest, factory.fromHttpServletRequest(httpReq).getBody());
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
