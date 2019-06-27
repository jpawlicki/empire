package com.empire;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

final class Request {
	final int turn;
	final int version;
	final long gameId;
	final String password;
	final String kingdom;
	final String body;
	final boolean skipMail;

	private Request(int turn, int version, long gameId, String password, String kingdom, String body, boolean skipMail) {
		this.turn = turn;
		this.version = version;
		this.gameId = gameId;
		this.password = password;
		this.kingdom = kingdom;
		this.body = body;
		this.skipMail = skipMail;
	}

	public static Request from(HttpServletRequest req) throws IOException {
		try {
			// Manual parsing is necessary: req.getParameter consumes the req inputstream.
			String path = req.getQueryString();
			String kingdom = extract("k", path, null);
			String password = extract("password", path, null);
			int version = Integer.parseInt(extract("v", path, "0"));
			int turn = Integer.parseInt(extract("t", path, "0"));
			long gameId = Long.parseLong(extract("gid", path, "-1"));
			boolean skipMail = "t".equals(extract("skipmail", path, "f"));
			return new Request(turn, version, gameId, password, kingdom, new String(getBody(req.getInputStream()), StandardCharsets.UTF_8), skipMail);
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	private static byte[] getBody(InputStream i) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		byte[] buffer = new byte[10000];
		int len;
		while((len = i.read(buffer)) > 0) {
			o.write(buffer, 0, len);
		}
		o.flush();
		return o.toByteArray();
	}

	private static String extract(String key, String query, String def) {
		if (query == null) return def;
		for (String param : query.split("&")) {
			if (param.startsWith(key + "=")) {
				try {
					return URLDecoder.decode(param.substring(key.length() + 1), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					return def;
				}
			}
		}
		return def;
	}

	/** Requests that don't specify the turn either are not turn-specific or they implicitly mean the latest turn. */
	public boolean hasTurn() {
		return turn != 0;
	}
}
