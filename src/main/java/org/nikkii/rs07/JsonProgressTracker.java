package org.nikkii.rs07;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.nikkii.rs07.event.LevelUpEvent;
import org.nikkii.rs07.event.OSBuddyEvent;
import org.nikkii.rs07.event.ParseEventError;
import org.nikkii.rs07.event.TreasureTrailEvent;
import org.nikkii.rs07.gallery.GalleryEntry;
import org.nikkii.rs07.gallery.GalleryEntrySorter;
import org.nikkii.rs07.gallery.GalleryJsonDeserializer;
import org.nikkii.rs07.http.HttpPostRequest;
import org.nikkii.rs07.http.HttpRequest;
import org.nikkii.rs07.http.data.RequestData;
import org.nikkii.rs07.http.multipart.HttpMultipartPostRequest;
import org.nikkii.rs07.http.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * @author Nikki
 */
public class JsonProgressTracker {

	private static final Gson gson = new GsonBuilder().registerTypeAdapter(GalleryEntry.class, new GalleryJsonDeserializer()).create();

	private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("screenshots[\\\\|\\/](.*?)[\\\\|\\/](.*?).png");

	private static final Pattern LEVEL_PATTERN = Pattern.compile("(.*?)\\sLevel\\s\\((\\d+)\\)");

	private static final Pattern TREASURE_TRAIL_PATTERN = Pattern.compile("Treasure Trail - (Easy|Medium|Hard|Elite) - (.*?)");

	private static final List<String> SKILLS = Arrays.asList(new String[]{
		"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Hitpoints", "Crafting", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting", "Runecraft"
		, "Agility", "Herblore", "Thieving", "Fletching", "Slayer", "Farming", "Construction", "Hunter"
	});

	private static final boolean PRODUCTION = true;
	private static final String PRODUCTION_URL = "http://rslog.cf/update";
	private static final String IMAGE_UPLOAD_URL = "http://rslog.cf/image/upload";
	private static final String DEV_URL = "http://127.0.0.1/update";

	private final AuthStore authStore;

	public static void main(String[] args) throws IOException {
		File home = new File(System.getProperty("user.home"));

		File osbuddyRoot = new File(home, "OSBuddy");

		if (!osbuddyRoot.exists()) {
			throw new IOException("Unable to find OSBuddy folder!");
		}

		File osbuddySub = new File(osbuddyRoot, "OSBuddy"), galleryFile1 = new File(osbuddyRoot, "gallery/data.json"), watchDir = galleryFile1.getParentFile();

		// Handle a weird case when using an old launcher, this has been fixed recently.
		if (osbuddySub.exists()) {
			File galleryFile2 = new File(osbuddySub, "gallery/data.json");

			if (galleryFile2.exists() && galleryFile2.lastModified() > galleryFile1.lastModified()) {
				watchDir = galleryFile2.getParentFile();
			}
		}

		JsonProgressTracker tracker = new JsonProgressTracker();

		tracker.track(watchDir);
	}

	private long lastModified = System.currentTimeMillis();

	private List<Integer> parsedEvents = new LinkedList<>();

	public JsonProgressTracker() throws IOException {
		this.authStore = AuthStore.load(new File(System.getProperty("user.home"), ".rslog"));
	}

	public void track(File dir) throws IOException {
		Path path = dir.toPath();

		WatchService watcher = FileSystems.getDefault().newWatchService();

		path.register(watcher, ENTRY_MODIFY);

		for (;;) {

			// wait for key to be signaled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();

				if (kind == OVERFLOW) {
					continue;
				}

				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();
				String name = filename.toString();

				if (!name.equals("data.json")) {
					continue;
				}

				if (kind == ENTRY_MODIFY) {
					try {
						handleFileModification(new File(dir, name));
					} catch (IOException ex) {
						System.out.println("Unknown error while parsing new data.");
					}
				}
			}

			// Reset the key -- this step is critical if you want to
			// receive further watch events.  If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}
	}

	private void handleFileModification(File file) throws IOException {
		long prevModified = lastModified;
		lastModified = System.currentTimeMillis();

		try (Reader reader = new FileReader(file)) {
			List<GalleryEntry> entries = gson.fromJson(reader, new TypeToken<List<GalleryEntry>>() {
			}.getType());
			Collections.sort(entries, new GalleryEntrySorter());

			for (ListIterator<GalleryEntry> it$ = entries.listIterator(entries.size()); it$.hasPrevious(); ) {
				GalleryEntry entry = it$.previous();

				if (entry.getTime() <= prevModified) {
					break;
				}

				OSBuddyEvent evt = parseEvent(entry);

				if (evt == null) {
					continue;
				}

				System.out.println("Event time: " + entry.getTime() + ", Last Modified: " + prevModified);

				if (evt == null || parsedEvents.contains(evt.hashCode())) continue;

				parsedEvents.add(evt.hashCode());

				submitProgress(evt, entry.getTime());
			}
		}
	}

	private void submitProgress(OSBuddyEvent evt, long time) {
		try {
			RequestData data = new RequestData();

			data.put("type", evt.getType())
				.put("displayName", evt.getDisplayName())
				.put("time", time / 1000L);

			if (authStore.hasAuth(evt.getDisplayName())) {
				data.put("key", authStore.getAuth(evt.getDisplayName()));
			}

			if (evt.hasScreenshot()) {
				data.put("url", uploadImage(evt.getDisplayName(), evt.getScreenshot()));
			}

			switch (evt.getType()) {
			case LEVEL_UP:
				LevelUpEvent levelUp = (LevelUpEvent) evt;

				data.put("skill", levelUp.getSkill())
					.put("level", levelUp.getLevel());
				System.out.println("[" + evt.getDisplayName() + "] Skill Level - Skill : " + levelUp.getSkill() + ", Level: " + levelUp.getLevel());
				break;
			case TREASURE_TRAIL:
				TreasureTrailEvent trail = (TreasureTrailEvent) evt;

				data.put("difficulty", trail.getDifficulty());
				System.out.println("[" + evt.getDisplayName() + "] Treasure Trail - Difficulty: " + trail.getDifficulty());
				break;
			}

			try (HttpPostRequest request = new HttpPostRequest(PRODUCTION ? PRODUCTION_URL : DEV_URL)) {
				request.setParameters(data);

				String body = request.getResponseBody();

				if (!body.equals("ok")) {
					body = body.trim();
					System.out.println("[" + evt.getDisplayName() + "] Got auth token " + body);
					authStore.setAuth(evt.getDisplayName(), body);
					authStore.save();
				} else {
					System.out.println("[" + evt.getDisplayName() + "] Update successfully pushed.");
				}
			}
		} catch(IOException e) {
			System.out.println("[" + evt.getDisplayName() + "] An error occurred while pushing the update.");
			e.printStackTrace();
		}
	}

	private OSBuddyEvent parseEvent(GalleryEntry entry) throws ParseEventError {
		Matcher m = DISPLAY_NAME_PATTERN.matcher(entry.getAbsolutePath());

		if (!m.find()) {
			return null;
		}

		String displayName = m.group(1), screenshotName = m.group(2);

		BufferedImage screenshot = null;

		// Attempt to load the screenshot

		try {
			screenshot = ImageIO.read(new File(entry.getAbsolutePath()));
		} catch (Exception e) {
			System.out.println("Unable to get screenshot.");
		}

		// Level up

		m = LEVEL_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String skill = m.group(1), level = m.group(2);

			if (!SKILLS.contains(skill)) {
				throw new ParseEventError("Unknown skill " + skill);
			}

			return new LevelUpEvent(displayName, entry.getTime(), screenshot, skill, Integer.parseInt(level));
		}

		// Treasure Trail

		m = TREASURE_TRAIL_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String difficulty = m.group(1);

			return new TreasureTrailEvent(displayName, entry.getTime(), screenshot, difficulty);
		}

		return null;
	}

	private String uploadImage(String displayName, BufferedImage image) throws IOException {
		try (HttpRequest request = new HttpMultipartPostRequest(IMAGE_UPLOAD_URL)) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, "PNG", output);

			RequestData data = new RequestData();
			data.put("displayName", AuthStore.formatName(displayName));
			data.put("image", new MultipartFile("progress.png", new ByteArrayInputStream(output.toByteArray())));
			request.setParameters(data);

			return request.getResponseBody();
		}
	}
}
