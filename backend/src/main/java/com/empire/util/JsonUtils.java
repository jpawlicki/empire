package com.empire.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

public class JsonUtils {
  private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  static JsonUtils factory = new JsonUtils();

  public static <T> String toJson(T obj) {
    return factory.objToJson(obj);
  }

  public static <T> T fromJson(String s, Class<T> clazz) {
    return factory.jsonToObj(s, clazz);
  }

  public static <T> T fromJson(String s, Type t) {
    return factory.jsonToObj(s, t);
  }

  <T> String objToJson(T obj) {
    return gson.toJson(obj);
  }

  <T> T jsonToObj(String s, Class<T> clazz) {
    return gson.fromJson(s, clazz);
  }

  <T> T jsonToObj(String s, Type t) {
    return gson.fromJson(s, t);
  }
}
