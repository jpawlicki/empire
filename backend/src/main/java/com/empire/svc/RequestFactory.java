package com.empire.svc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;

class RequestFactory {
  static RequestFactory factory = new RequestFactory();

  static String kingdomKey = "k";
  static String passwordKey = "password";
  static String versionKey = "v";
  static String turnKey = "t";
  static String gameIdKey = "gid";
  static String skipmailKey = "skipmail";

  public static Request from(HttpServletRequest req) throws IOException {
    return factory.fromHttpServletRequest(req);
  }

  Request fromHttpServletRequest(HttpServletRequest req) throws IOException {
    try {
      // Manual parsing is necessary: req.getParameter consumes the req inputstream.
      String path = req.getQueryString();
      String kingdom = extract(RequestFactory.kingdomKey, path, null);
      String password = extract(RequestFactory.passwordKey, path, null);
      int version = Integer.parseInt(extract(RequestFactory.versionKey, path, "0"));
      int turn = Integer.parseInt(extract(RequestFactory.turnKey, path, "0"));
      long gameId = Long.parseLong(extract(gameIdKey, path, "-1"));
      boolean skipMail = "t".equals(extract(skipmailKey, path, "f"));
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
}
