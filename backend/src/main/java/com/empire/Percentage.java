package com.empire;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/** A double value, clamped between 0 and 1 (inclusive), but that can track overflows. */
class Percentage {
	private double trueValue;

	public Percentage(double val) {
		trueValue = val;
	}

	public Percentage add(double amt) {
		trueValue += amt;
		return this;
	}

	public double get() {
		return Math.min(1, Math.max(0, trueValue));
	}

	public Percentage set(double amt) {
		trueValue = amt;
		return this;
	}

	static class TypeAdapter implements JsonSerializer<Percentage>, JsonDeserializer<Percentage> {
		@Override
		public JsonElement serialize(Percentage p, Type t, JsonSerializationContext context) {
    	return new JsonPrimitive(p.get());
   	}

		@Override
		public Percentage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new Percentage(json.getAsDouble());
		}
	}
}
