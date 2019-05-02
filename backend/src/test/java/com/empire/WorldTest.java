package com.empire;

import com.google.appengine.repackaged.com.google.common.io.Resources;
import com.google.common.base.Charsets;

import java.io.IOException;

public class WorldTest {
	public static String armyWorldJson;
	public static String emptyWorldJson;

	static{
		armyWorldJson = readResourceAsString("ArmyWorldTest.json");
		emptyWorldJson = readResourceAsString("EmptyWorldTest.json");
	}

	private static String readResourceAsString(String resource){
		try {
			return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
		} catch(IOException e){
			return null;
		}
	}

	static World armyTestWorld() {
		return World.fromJson(armyWorldJson);
	}

	static World emptyTestWorld() {
		return World.fromJson(emptyWorldJson);
	}
}
