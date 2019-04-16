package com.empire;

public class WorldTest {
	static World makeTestWorld() {
		return World.fromJson(
			"{"
					+ "\"kingdoms\": {"
					+ "  \"k1\": {"
					+ "  }"
					+ "},"
					+ "\"regions\": ["
					+ "  {"
					+	"    \"kingdom\": \"k1\","
					+	"    \"type\": \"land\""
					+ "  }"
					+ "]"
					+ "}");
	}
}
