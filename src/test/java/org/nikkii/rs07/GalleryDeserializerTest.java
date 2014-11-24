package org.nikkii.rs07;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import org.nikkii.rs07.gallery.GalleryEntry;
import org.nikkii.rs07.gallery.GalleryEntrySorter;
import org.nikkii.rs07.gallery.GalleryJsonDeserializer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikki
 */
public class GalleryDeserializerTest {
	@Test
	public void testDeserialize() throws IOException {
		ProgressTrackerSettings settings;
		try (Reader reader = new InputStreamReader(GalleryDeserializerTest.class.getResourceAsStream("/settings.json"))) {
			settings = new Gson().fromJson(reader, ProgressTrackerSettings.class);
		}

		Gson gson = new GsonBuilder().registerTypeAdapter(GalleryEntry.class, new GalleryJsonDeserializer(settings.getDeserializerSettings())).create();

		try (Reader reader = new FileReader(new File(System.getProperty("user.home"), "OSBuddy/gallery/data.json"))) {
			List<GalleryEntry> entries = gson.fromJson(reader, new TypeToken<List<GalleryEntry>>() {
			}.getType());
			Collections.sort(entries, new GalleryEntrySorter());

			for (GalleryEntry entry : entries) {
				if (entry.getDeletionKey() != null) {
					System.out.println(entry);
				}
			}
		}
	}
}
