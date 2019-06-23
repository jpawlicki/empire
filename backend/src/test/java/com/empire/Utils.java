package com.empire;

import com.google.appengine.repackaged.com.google.common.io.Resources;
import com.google.common.base.Charsets;
import java.io.IOException;

public class Utils {
	public static final Rules rules;
	static {
		Rules r = null;
		try {
			r = Rules.loadRules(5);
		} catch (IOException e) {
			e.printStackTrace();
		}
		rules = r;
	}

	public static String readResourceAsString(String resource) {
		try {
			return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
		} catch(IOException e){
			return null;
		}
	}
}
