package com.empire.svc;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.World;
import com.empire.util.JsonUtilsTest;
import com.empire.util.JsonUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntryServletTest {
  private EntryServlet servlet;
  private EntryServletBackend backend;
  private HttpServletRequest httpReq;
  private HttpServletResponse httpResp;
  private ServletOutputStream os;
  private Request req;
  private JsonUtils json;

  private final String jsonTestResp = "{\"status\": \"success\"}";
  private final byte[] jsonTestBytes = jsonTestResp.getBytes(StandardCharsets.UTF_8);

  @Before
  public void setup() {
    backend = mock(EntryServletBackend.class);
    servlet = new EntryServlet(backend);

    httpReq = mock(HttpServletRequest.class);
    RequestFactory.factory = mock(RequestFactory.class);
    req = mock(Request.class);

    try {
      when(RequestFactory.factory.fromHttpServletRequest(httpReq)).thenReturn(req);
    } catch (IOException e) {
      fail("IOException when mocking RequestFactory.factory.fromHttpServletRequest()");
    }

    httpResp = mock(HttpServletResponse.class);
    os = mock(ServletOutputStream.class);

    try {
      when(httpResp.getOutputStream()).thenReturn(os);
    } catch (IOException e) {
      fail("IOException when mocking httpResp.getOutputStream");
    }

    json = mock(JsonUtils.class);
    JsonUtilsTest.setJsonUtilInstance(json);
  }

  /**
   * This is our definition of success for the get methods:
   *  No errors are sent
   *  A status of 200 is sent
   *  The return body is added to the Response
   */
  private void assertGetSuccess() throws IOException {
    verify(httpResp, never()).sendError(Mockito.anyInt(), Mockito.anyString());
    verify(httpResp).setStatus(200);
    verify(os).flush();
    verify(os).write(jsonTestBytes);
    verify(os).flush();
  }

  /** This is our definition of success for the post methods */
  private void assertPostSuccess() throws IOException {
    verify(httpResp, never()).sendError(Mockito.anyInt(), Mockito.anyString());
    verify(httpResp).setStatus(204);
  }

  /** This is our definition of failure for the get methods */
  private void assertRequestFailure() throws IOException {
    verify(httpResp).sendError(eq( 404), Mockito.anyString());
  }

  @Test
  public void getWithBadPathReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn("BOGUS");

    try {
      servlet.doGet(httpReq, httpResp);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void postWithBadPathReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn("BOGUS");

    try {
      servlet.doPost(httpReq, httpResp);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void successfulPingRequestReturnsSuccessResponse(){
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.pingRoute);

    try {
      servlet.doGet(httpReq, httpResp);
      verify(httpResp, never()).sendError(Mockito.anyInt(), Mockito.anyString());
      verify(httpResp).setStatus(200);
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void getOrdersBackendEmptyReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.ordersRoute);
    when(backend.getOrders(req)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getOrders(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void getOrdersBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.ordersRoute);

    Map<String, String> ordersMap = new HashMap<>();
    Orders orders = mock(Orders.class);
    when(orders.getVersion()).thenReturn(42);
    when(orders.getOrders()).thenReturn(ordersMap);
    when(backend.getOrders(req)).thenReturn(Optional.of(orders));

    JsonUtilsTest.mockToJson(json, ordersMap, jsonTestResp);

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getOrders(req);
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getSetupBackendEmptyReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.setupRoute);
    when(backend.getSetup(req)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getSetup(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getSetupBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.setupRoute);

    Nation nation = mock(Nation.class);
    when(backend.getSetup(req)).thenReturn(Optional.of(nation));

    JsonUtilsTest.mockToJson(json, nation, jsonTestResp);

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getSetup(req);
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getWorldBackendEmptyReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.worldRoute);
    when(backend.getWorld(req)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getWorld(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getWorldBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.worldRoute);

    World world = mock(World.class);
    when(backend.getWorld(req)).thenReturn(Optional.of(world));

    JsonUtilsTest.mockToJson(json, world, jsonTestResp);

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getWorld(req);
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getAdvancePollBackendEmptyReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.advanceWorldPollRoute);
    when(backend.getAdvancePoll()).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getAdvancePoll();
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getAdvancePollBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.advanceWorldPollRoute);
    when(backend.getAdvancePoll()).thenReturn(Optional.of(jsonTestResp));

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getAdvancePoll();
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getActivityBackendEmptyReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.activityRoute);
    when(backend.getActivity(req)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getActivity(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getActivityBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.activityRoute);

    List<Map<String, Boolean>> activityList = new ArrayList<>();
    when(backend.getActivity(req)).thenReturn(Optional.of(activityList));

    JsonUtilsTest.mockToJson(json, activityList, jsonTestResp);

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getActivity(req);
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postOrdersBackendFailReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.ordersRoute);
    when(backend.postOrders(req)).thenReturn(false);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postOrders(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postOrdersBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.ordersRoute);
    when(backend.postOrders(req)).thenReturn(true);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postOrders(req);
      assertPostSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postAdvanceWorldBackendFailReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.advanceWorldRoute);
    when(backend.postAdvanceWorld(req)).thenReturn(false);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postAdvanceWorld(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postAdvanceWorldFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.advanceWorldRoute);
    when(backend.postAdvanceWorld(req)).thenReturn(true);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postAdvanceWorld(req);
      assertPostSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postStartWorldBackendFailReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.startWorldRoute);
    when(backend.postStartWorld(req)).thenReturn(false);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postStartWorld(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postStartWorldFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.startWorldRoute);
    when(backend.postStartWorld(req)).thenReturn(true);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postStartWorld(req);
      assertPostSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postSetupBackendFailReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.setupRoute);
    when(backend.postSetup(req)).thenReturn(false);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postSetup(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postSetupFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.setupRoute);
    when(backend.postSetup(req)).thenReturn(true);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postSetup(req);
      assertPostSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postRtcBackendFailReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.rtcRoute);
    when(backend.postRealTimeCommunication(req)).thenReturn(false);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postRealTimeCommunication(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postRtcFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.rtcRoute);
    when(backend.postRealTimeCommunication(req)).thenReturn(true);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postRealTimeCommunication(req);
      assertPostSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postChangePlayerBackendFailReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.changePlayerRoute);
    when(backend.postChangePlayer(req)).thenReturn(false);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postChangePlayer(req);
      assertRequestFailure();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void postChangePlayerReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.changePlayerRoute);
    when(backend.postChangePlayer(req)).thenReturn(true);

    try {
      servlet.doPost(httpReq, httpResp);
      verify(backend).postChangePlayer(req);
      assertPostSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }
}
