package com.empire.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

public class JsonUtils {
  private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

  public static <T> String toJson(T obj) {
    return gson.toJson(obj);
  }

  public static <T> T fromJson(String s, Class<T> clazz) {
    return gson.fromJson(s, clazz);
  }

  public static <T> T fromJson(String s, Type t) {
    return gson.fromJson(s, t);
  }
}
