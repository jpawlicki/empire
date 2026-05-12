package com.empire;

import com.google.gson.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public class OptionalAdapter implements JsonSerializer<Optional<?>>, JsonDeserializer<Optional<?>> {
	@Override
	public JsonElement serialize(Optional<?> src, Type typeOfSrc, JsonSerializationContext context) {
		return src.map(context::serialize).orElse(JsonNull.INSTANCE);
	}

	@Override
	public Optional<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Type innerType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
		if (json.isJsonNull()) {
			return Optional.empty();
		} else {
			return Optional.of(context.deserialize(json, innerType));
		}
	}
}
