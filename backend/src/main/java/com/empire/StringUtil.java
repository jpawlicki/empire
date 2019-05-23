package com.empire;

import java.util.List;

public class StringUtil {
	public static String and(List<String> in) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < in.size(); i++) {
			if (i > 0 && in.size() > 2) b.append(",");
			if (i > 0) b.append(" ");
			if (i > 0 && i == in.size() - 1) b.append("and ");
			b.append(in.get(i));
		}
		return b.toString();
	}

	public static String quantify(int num, String unit) {
		if (unit.endsWith("s")) return num + " " + unit;
		return num + " " + unit + (num != 1 ? "s" : "");
	}

	private StringUtil() {}
}
