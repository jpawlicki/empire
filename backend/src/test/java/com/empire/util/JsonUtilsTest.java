package com.empire.util;

import java.lang.reflect.Type;
import static org.mockito.Mockito.when;

public class JsonUtilsTest {
  public static void setJsonUtilInstance(JsonUtils jsonUtil) {
    JsonUtils.factory = jsonUtil;
  }

  public static <T> void mockToJson(JsonUtils jsonUtil, T obj, String json) {
    when(jsonUtil.objToJson(obj)).thenReturn(json);
  }

  public static <T> void mockFromJson(JsonUtils jsonUtil, String json, Class<T> clazz, T obj) {
    when(jsonUtil.jsonToObj(json, clazz)).thenReturn(obj);
  }

  public static <T> void mockFromJson(JsonUtils jsonUtil, String json, Type t, T obj) {
    when(jsonUtil.jsonToObj(json, t)).thenReturn(obj);
  }
}
