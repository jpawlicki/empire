package com.empire;

import com.google.appengine.repackaged.com.google.common.io.Resources;
import com.google.common.base.Charsets;
import java.io.IOException;
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

	public static String readResourceAsString(String resource) {
		try {
			return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
		} catch(IOException e){
			return null;
		}
	}

	private StringUtil() {}
}
