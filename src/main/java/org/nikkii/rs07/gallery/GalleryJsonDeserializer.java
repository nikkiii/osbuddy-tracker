package org.nikkii.rs07.gallery;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Nikki
 */
public class GalleryJsonDeserializer implements JsonDeserializer<GalleryEntry> {
	@Override
	public GalleryEntry deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject obj = element.getAsJsonObject();

		long time = obj.get("InsertWittyName_a").getAsLong();
		String absolutePath = obj.get("InsertWittyName_b").getAsString(), fileName = obj.get("InsertWittyName_c").getAsString();

		return new GalleryEntry(time, absolutePath, fileName);
	}
}
