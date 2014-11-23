package org.nikkii.rs07;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.nikkii.rs07.event.DuelVictoryEvent;
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
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * @author Nikki
 */
public class JsonProgressTracker {

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

		tracker.initializeTrayIcon();

		tracker.track(watchDir);
	}

	/**
	 * The RegExp used to parse out the player's display name.
	 * This can be either the windows or unix type with path separator.
	 */
	private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("screenshots[\\\\|\\/](.*?)[\\\\|\\/](.*?).png");

	/**
	 * The level pattern.
	 *
	 * Example: Hunter Level (99)
	 */
	private static final Pattern LEVEL_PATTERN = Pattern.compile("(.*?)\\sLevel\\s\\((\\d+)\\)");

	/**
	 * The Treasure Trail pattern.
	 *
	 * Example: Treasure Trail - Elite - <date>
	 */
	private static final Pattern TREASURE_TRAIL_PATTERN = Pattern.compile("Treasure Trail - (Easy|Medium|Hard|Elite) - (.*?)");

	/**
	 * The Duel Victory pattern.
	 *
	 * This is currently broken due to some weird characters in the duel victory names.
	 */
	private static final Pattern DUEL_VICTORY_PATTERN = Pattern.compile("^Victory against (.*?) - ([0-9\\._-]+)$");

	/**
	 * A list of valid skills.
	 */
	private static final List<String> SKILLS = Arrays.asList(new String[]{
		"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Hitpoints", "Crafting", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting", "Runecraft"
		, "Agility", "Herblore", "Thieving", "Fletching", "Slayer", "Farming", "Construction", "Hunter"
	});

	/**
	 * The Gson deserializer with registered gallery deserializer.
	 */
	private final Gson gson;

	/**
	 * The AuthStore instance.
	 */
	private final AuthStore authStore;

	/**
	 * The progress tracker settings.
	 */
	private final ProgressTrackerSettings settings;

	/**
	 * The time to use when checking for new entries.
	 */
	private long lastModified = System.currentTimeMillis();

	/**
	 * The list of previously parsed events, to make sure we don't parse duplicates.
	 */
	private final List<Integer> parsedEvents = new LinkedList<>();

	/**
	 * The worker to submit our queue of updates.
	 */
	private final UpdateQueueWorker worker = new UpdateQueueWorker(this);

	/**
	 * Construct a new progress tracker.
	 *
	 * @throws IOException If an error occurs loading auth files or reading settings.
	 */
	public JsonProgressTracker() throws IOException {
		try (Reader reader = new InputStreamReader(JsonProgressTracker.class.getResourceAsStream("/settings.json"))) {
			this.settings = new Gson().fromJson(reader, ProgressTrackerSettings.class);
		}

		System.out.println(settings.getDeserializerSettings());
		gson = new GsonBuilder().registerTypeAdapter(GalleryEntry.class, new GalleryJsonDeserializer(settings.getDeserializerSettings())).create();

		this.authStore = AuthStore.load(new File(System.getProperty("user.home"), ".rslog"));
		new Thread(worker).start();
	}

	/**
	 * Initialize the tray icon.
	 *
	 * @throws IOException If an error occurs reading the icon file.
	 */
	private void initializeTrayIcon() throws IOException {
		TrayIcon icon = new TrayIcon(ImageIO.read(JsonProgressTracker.class.getResourceAsStream("/icon.png")));

		PopupMenu menu = new PopupMenu();

		MenuItem exit = new MenuItem("Exit");

		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(1);
			}
		});

		menu.add(exit);

		icon.setPopupMenu(menu);

		try {
			SystemTray.getSystemTray().add(icon);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Track a directory.
	 *
	 * @param dir The screenshot gallery directory.
	 * @throws IOException If an error occurs while tracking file modifications.
	 */
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

	/**
	 * Parse the file after it was modified, checking new events.
	 *
	 * @param file The file which was modified.
	 * @throws IOException if an error occurs while reading or deserializing the file.
	 */
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

				if (evt == null || parsedEvents.contains(evt.hashCode())) continue;

				parsedEvents.add(evt.hashCode());

				worker.queue(evt);
			}
		}
	}

	/**
	 * Submit a progress event to the web service.
	 *
	 * @param evt The event to submit.
	 */
	public void submitProgress(OSBuddyEvent evt) {
		try {
			RequestData data = new RequestData();

			data.put("type", evt.getType())
				.put("displayName", evt.getDisplayName())
				.put("time", evt.getEntry().getTime() / 1000L);

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
			case DUEL_VICTORY:
				DuelVictoryEvent victory = (DuelVictoryEvent) evt;

				data.put("opponent", victory.getOpponent());
				System.out.println("[" + evt.getDisplayName() + "] Duel Victory - Opponent: " + victory.getOpponent());
				break;
			}

			try (HttpPostRequest request = new HttpPostRequest(settings.getUpdateUrl())) {
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

	/**
	 * Parse an osbuddy gallery entry into an event.
	 *
	 * @param entry The gallery entry.
	 * @return The parsed event, or null if unable to find one.
	 * @throws ParseEventError If an error occurs while parsing the event (invalid skill, etc)
	 */
	private OSBuddyEvent parseEvent(GalleryEntry entry) throws ParseEventError {
		Matcher m = DISPLAY_NAME_PATTERN.matcher(entry.getAbsolutePath());

		if (!m.find()) {
			return null;
		}

		String displayName = m.group(1), screenshotName = m.group(2);

		BufferedImage screenshot = null;

		// Attempt to load the screenshot

		try {
			screenshot = entry.getScreenshot();
		} catch (Exception e) {
			System.out.println("Unable to get screenshot from " + entry.getAbsolutePath());
		}

		// Level up

		m = LEVEL_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String skill = m.group(1), level = m.group(2);

			if (!SKILLS.contains(skill)) {
				throw new ParseEventError("Unknown skill " + skill);
			}

			return new LevelUpEvent(entry, displayName, screenshot, skill, Integer.parseInt(level));
		}

		// Treasure Trail

		m = TREASURE_TRAIL_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String difficulty = m.group(1);

			return new TreasureTrailEvent(entry, displayName, screenshot, difficulty);
		}

		// Duel victory

		/*m = DUEL_VICTORY_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String opponent = m.group(1);

			return new DuelVictoryEvent(entry, displayName, screenshot, opponent);
		}*/

		return null;
	}

	/**
	 * Upload an image to the image service.
	 *
	 * @param displayName The player display name.
	 * @param image The image.
	 * @return The uploaded URL.
	 * @throws IOException If an error occurs while uploading.
	 */
	private String uploadImage(String displayName, BufferedImage image) throws IOException {
		try (HttpRequest request = new HttpMultipartPostRequest(settings.getImageUrl())) {
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
