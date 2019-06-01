package com.empire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

class Nation {
	
	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	static final class NationGson {
		public static String TYPE = "Nation";
		public String rulerName = "";
		public String title = "";
		public String food = "";
		public String happiness = "";
		public String territory = "";
		public String glory = "";
		public String religion = "";
		public String ideology = "";
		public String security = "";
		public String riches = "";
		public String culture = "";
		public NationData.Tag trait1;
		public NationData.Tag trait2;
		public Ideology dominantIdeology;
		public String bonus = "";
		public String email = "";
		public String password = "";

		public static String loadJson(String nation, long gameId, DatastoreService service) throws EntityNotFoundException {
			Entity e = service.get(KeyFactory.createKey(TYPE, gameId + "_" + nation));
			return ((String)e.getProperty("json"));
		}

		public static NationGson loadNation(String nation, long gameId, DatastoreService service) throws EntityNotFoundException {
			return fromJson(loadJson(nation, gameId, service));
		}

		public static NationGson fromJson(String json) {
			return getGson().fromJson(json, NationGson.class);
		}

		public Entity toEntity(String nation, long gameId) {
			Entity e = new Entity(TYPE, gameId + "_" + nation);
			e.setProperty("json", getGson().toJson(this));
			return e;
		}

		public boolean hasTag(NationData.Tag tag) {
			return trait1 == tag || trait2 == tag;
		}

		@Override
		public String toString() {
			return getGson().toJson(this);
		}
	}
}
