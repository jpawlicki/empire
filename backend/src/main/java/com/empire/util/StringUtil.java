package com.empire.util;

import java.util.Collection;

public class StringUtil {
	public static String and(Collection<String> in) {
		StringBuilder b = new StringBuilder();
		int i = 0;
		for (String s : in) {
			if (i > 0 && in.size() > 2) b.append(",");
			if (i > 0) b.append(" ");
			if (i > 0 && i == in.size() - 1) b.append("and ");
			b.append(s);
			i++;
		}
		return b.toString();
	}

	public static String quantify(int num, String unit) {
		if (unit.endsWith("s")) return num + " " + unit;
		return num + " " + unit + (num != 1 ? "s" : "");
	}

	public static String sanitizeName(String in) {
		for (String remove : new String[]{"<", ">", "\t", "\n"}) in = in.replaceAll(remove, "");
		return in.trim();
	}

	private StringUtil() {}
}
