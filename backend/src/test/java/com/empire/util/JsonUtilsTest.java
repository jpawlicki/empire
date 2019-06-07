package com.empire.util;

import static org.mockito.Mockito.when;

public class JsonUtilsTest {
  public static void setJsonUtilInstance(JsonUtils jsonUtil) {
    JsonUtils.factory = jsonUtil;
  }

  public static <T> void mockToJson(JsonUtils jsonUtil, T obj, String json) {
    when(jsonUtil.objToJson(obj)).thenReturn(json);
  }
}
