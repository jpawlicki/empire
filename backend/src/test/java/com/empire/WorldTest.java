package com.empire;

import com.google.appengine.repackaged.com.google.common.io.Resources;
import com.google.common.base.Charsets;
import java.io.IOException;

public class WorldTest {
	public static final String basicWorldJson = readResourceAsString("BasicWorldTest.json");
	public static final String armyWorldJson = readResourceAsString("ArmyWorldTest.json");

	private static String readResourceAsString(String resource) {
		try {
			return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
		} catch(IOException e){
			return null;
		}
	}

	static World basicTestWorld() {
		return World.fromJson(basicWorldJson);
	}

	static World armyTestWorld() {
		return World.fromJson(armyWorldJson);
	}
}
