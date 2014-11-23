package org.nikkii.rs07.gallery;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.nikkii.rs07.DeserializerSettings;

import java.io.File;
import java.lang.reflect.Type;

/**
 * A {@link JsonDeserializer} to deserialize Gallery entries. OSBuddy uses a weird naming scheme, likely obfuscated field names.
 *
 * @author Nikki
 */
public class GalleryJsonDeserializer implements JsonDeserializer<GalleryEntry> {

	private final DeserializerSettings settings;

	public GalleryJsonDeserializer(DeserializerSettings settings) {
		this.settings = settings;
	}

	@Override
	public GalleryEntry deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject obj = element.getAsJsonObject();

		long time = obj.get(settings.getTimeField()).getAsLong();
		String absolutePath = obj.get(settings.getAbsolutePathField()).getAsString(),
			   fileName = obj.get(settings.getFileNameField()).getAsString();

		fileName = fileName.replaceAll("[^a-zA-Z0-9\\-_\\(\\) \\.]", " ");

		absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separatorChar) + 1) + fileName;

		return new GalleryEntry(time, absolutePath, fileName);
	}
}
