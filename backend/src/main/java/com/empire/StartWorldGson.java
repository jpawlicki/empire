package com.empire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import java.util.List;
import java.util.ArrayList;

public class StartWorldGson {
	public String gmPassword;
	public String obsPassword;
	public List<String> kingdoms = new ArrayList<String>(); 
	public Schedule schedule;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	public static StartWorldGson fromJson(String json) {
		return getGson().fromJson(json, StartWorldGson.class);
	}
}
