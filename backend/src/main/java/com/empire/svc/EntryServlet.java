package com.empire.svc;

import com.empire.Orders;
import com.empire.util.JsonUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
Basic design:
GET /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and return the given orders entry JSON.
GET /entry/setup?gid=1234&k=Aefoss
  Return customization info for kingdom. If none, empty string.
GET /entry/world?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and return the visible world view JSON.
GET /entry/advancegamepoll
  Advance all games one turn.

POST /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and post the given order data.
POST /entry/setup?gid=1234&k=Aefoss&t=0&password=foobar
	Set customization info for the kingdom, if unset.
POST /entry/advanceworld?gid=1234
	Check cadence and possibly advance world to next step, mail players.
POST /entry/startworld?gid=1234
	Start a new game.

TODO: Will eventually need a changePassword/change-email.
*/

// TODO: Unit tests for EntryServlet

@WebServlet(name = "EntryServlet", value = "/entry/*")
public class EntryServlet extends HttpServlet {
//	private static final Logger log = Logger.getLogger(EntryServlet.class.getName());

	private static final String routePrefix = "/entry";
	static final String pingRoute = routePrefix + "/ping";
	static final String ordersRoute = routePrefix + "/orders";
	static final String setupRoute = routePrefix + "/setup";
	static final String worldRoute = routePrefix + "/world";
	static final String startWorldRoute = routePrefix + "/startworld";
	static final String advanceWorldRoute = routePrefix + "/advanceworld";
	static final String advanceWorldPollRoute = routePrefix + "/advanceworldpoll";
	static final String rtcRoute = routePrefix + "/rtc";
	static final String activityRoute = routePrefix + "/activity";
	static final String changePlayerRoute = routePrefix + "/changeplayer";

	private final EntryServletBackend backend;

	/* This constructor is needed in order to start the service with GoogleAppEngine */
	public EntryServlet(){
		this(EntryServletBackend.create());
	}

	/* This constructor is needed to enable testing */
	EntryServlet(EntryServletBackend backend){
		super();
		this.backend = backend;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request r = RequestFactory.from(req);
		resp.setHeader("Access-Control-Allow-Origin", "*");

		Optional<String> jsonOpt;

		switch(req.getRequestURI()) {
			case EntryServlet.pingRoute:
				jsonOpt = Optional.of("");
				break;
			case EntryServlet.ordersRoute:
				Optional<Orders> ordersOpt = backend.getOrders(r);
				ordersOpt.ifPresent(o -> resp.setHeader("SJS-Version", String.valueOf(ordersOpt.get().getVersion())));
				jsonOpt = ordersOpt.map(Orders::getOrders).map(JsonUtils::toJson);
				break;
			case EntryServlet.setupRoute:
				jsonOpt = backend.getSetup(r).map(JsonUtils::toJson);
				break;
			case EntryServlet.worldRoute:
				jsonOpt = backend.getWorld(r).map(JsonUtils::toJson);
				break;
			case EntryServlet.advanceWorldPollRoute:
				jsonOpt = backend.getAdvancePoll();
				break;
			case EntryServlet.activityRoute:
				jsonOpt = backend.getActivity(r).map(JsonUtils::toJson);
				break;
			default:
				resp.sendError(404, "No such path.");
				return;
		}

		if (!jsonOpt.isPresent()) {
			resp.sendError(404, "No such entity.");
			return;
		}

		resp.setHeader("Access-Control-Expose-Headers", "SJS-Version");
		resp.setContentType("application/json");
		byte[] jsonBytes = jsonOpt.get().getBytes(StandardCharsets.UTF_8);
		resp.setContentLength(jsonBytes.length);
		OutputStream os = resp.getOutputStream();
		os.write(jsonBytes);
		os.flush();
  }

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		Request r = RequestFactory.from(req);

		boolean success = false;
		String err;

		switch(req.getRequestURI()) {
			case EntryServlet.ordersRoute:
				success = backend.postOrders(r);
				err = "Failure.";
				break;
			case EntryServlet.advanceWorldRoute:
				success = backend.postAdvanceWorld(r);
				err = "Failure.";
				break;
			case EntryServlet.startWorldRoute:
				success = backend.postStartWorld(r);
				err = "Failure.";
				break;
			case EntryServlet.setupRoute:
				success = backend.postSetup(r);
				err = "Not allowed.";
				break;
			case EntryServlet.rtcRoute:
				success = backend.postRealTimeCommunication(r);
				err = "Not allowed.";
				break;
			case EntryServlet.changePlayerRoute:
				success = backend.postChangePlayer(r);
				err = "Not allowed.";
				break;
			default:
				err = "No such path.";
				break;
		}

		if (success) {
			resp.setStatus(204);
		} else {
			resp.sendError(400, err);
		}
	}

	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		resp.addHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type");
		super.doOptions(req, resp);
	}
}
