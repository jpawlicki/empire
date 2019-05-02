package com.empire;

import com.google.appengine.repackaged.com.google.common.io.Resources;
import com.google.common.base.Charsets;

import java.io.IOException;

public class WorldTest {
	public static String worldJson;

	static{
		try {
			worldJson = Resources.toString(Resources.getResource("TestWorld.json"), Charsets.UTF_8);
		} catch(IOException e){
			worldJson = null;

		}
	}

	static World makeTestWorld() {
		return World.fromJson(worldJson);
	}
}
