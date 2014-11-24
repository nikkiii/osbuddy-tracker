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

		JsonElement timeElement = obj.get(settings.getTimeField());
		JsonElement absolutePathElement = obj.get(settings.getAbsolutePathField());
		JsonElement fileNameElement = obj.get(settings.getFileNameField());
		JsonElement deletionKeyElement = obj.get(settings.getDeletionKeyField());
		JsonElement urlElement = obj.get(settings.getUrlField());

		long time = timeElement.getAsLong();
		String absolutePath = absolutePathElement.getAsString(),
			   fileName = fileNameElement.getAsString(),
			   deletionKey = deletionKeyElement != null ? deletionKeyElement.getAsString() : null,
			   url = urlElement != null ? urlElement.getAsString() : null;

		fileName = fileName.replaceAll("[^a-zA-Z0-9\\-_\\(\\) \\.]", " ");

		absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separatorChar) + 1) + fileName;

		return new GalleryEntry(time, absolutePath, fileName, deletionKey, url);
	}
}
