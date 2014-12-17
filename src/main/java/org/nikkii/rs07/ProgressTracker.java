package org.nikkii.rs07;

import com.google.gson.Gson;
import org.nikkii.rs07.event.DuelVictoryEvent;
import org.nikkii.rs07.event.LevelUpEvent;
import org.nikkii.rs07.event.OSBuddyEvent;
import org.nikkii.rs07.event.ParseEventError;
import org.nikkii.rs07.event.TreasureTrailEvent;
import org.nikkii.rs07.http.HttpPostRequest;
import org.nikkii.rs07.http.HttpRequest;
import org.nikkii.rs07.http.data.RequestData;
import org.nikkii.rs07.http.multipart.HttpMultipartPostRequest;
import org.nikkii.rs07.http.multipart.MultipartFile;
import org.nikkii.rs07.util.DesktopEntryBuilder;
import org.nikkii.rs07.util.Util;
import org.nikkii.rs07.util.Util.OperatingSystem;
import org.nikkii.rs07.util.WinRegistry;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A progress tracker which watches directories using java's nio watchservice.
 *
 * @author Nikki
 */
public class ProgressTracker {

	private static final Logger logger = Logger.getLogger(ProgressTracker.class.getName());

	private static final String APPLICATION_NAME = "RSLog";

	public static void main(String[] args) throws IOException {
		logger.info("Finding OSBuddy directory...");

		File home = new File(System.getProperty("user.home"));

		File osbuddyRoot = new File(home, "OSBuddy");

		if (!osbuddyRoot.exists()) {
			throw new IOException("Unable to find OSBuddy folder!");
		}

		File watchDir = new File(osbuddyRoot, "screenshots");

		ProgressTracker tracker = new ProgressTracker();

		logger.info("Checking startup...");
		tracker.checkStartup();

		logger.info("Initializing tray icon...");
		tracker.initializeTrayIcon();

		logger.info("Starting watch service...");
		tracker.track(watchDir);
	}

	/**
	 * The level pattern.
	 *
	 * Example: Hunter Level (99)
	 */
	private static final Pattern LEVEL_PATTERN = Pattern.compile("^(.*?)\\sLevel\\s\\((\\d+)\\)$");

	/**
	 * The Treasure Trail pattern.
	 *
	 * Example: Treasure Trail - Elite - <date>
	 */
	private static final Pattern TREASURE_TRAIL_PATTERN = Pattern.compile("^Treasure Trail - (Easy|Medium|Hard|Elite) - (.*?)$");

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
	 * The Gson instance.
	 */
	private final Gson gson = new Gson();

	/**
	 * The AuthStore instance.
	 */
	private final AuthStore authStore;

	/**
	 * The progress tracker settings.
	 */
	private final ProgressTrackerSettings settings;

	/**
	 * The progress tracker user settings.
	 */
	private final ProgressTrackerUserSettings userSettings;


	private final ExecutorService watchService = Executors.newCachedThreadPool();

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
	public ProgressTracker() throws IOException {
		try (Reader reader = new InputStreamReader(ProgressTracker.class.getResourceAsStream("/settings.json"))) {
			this.settings = gson.fromJson(reader, ProgressTrackerSettings.class);
		}

		File rslogDirectory = new File(System.getProperty("user.home"), ".rslog");
		File rslogSettings = new File(rslogDirectory, "settings.json");

		if (rslogSettings.exists()) {
			try (Reader reader = new FileReader(rslogSettings)) {
				this.userSettings = gson.fromJson(reader, ProgressTrackerUserSettings.class);
			}
		} else {
			this.userSettings = new ProgressTrackerUserSettings();

			saveUserSettings();
		}

		this.authStore = AuthStore.load(rslogDirectory);
		new Thread(worker).start();
	}

	/**
	 * Save the user settings to file.
	 * @throws IOException If an error occurs while writing to the file.
	 */
	private void saveUserSettings() throws IOException {
		File settingsDirectory = new File(System.getProperty("user.home"), ".rslog");

		File settingsFile =  new File(settingsDirectory, "settings.json");
		try (Writer writer = new FileWriter(settingsFile)) {
			gson.toJson(userSettings, writer);
		}
	}

	/**
	 * Initialize the tray icon.
	 *
	 * @throws IOException If an error occurs reading the icon file.
	 */
	private void initializeTrayIcon() throws IOException {
		Image image = ImageIO.read(ProgressTracker.class.getResourceAsStream("/icon.png"));

		Dimension size = SystemTray.getSystemTray().getTrayIconSize();

		image = image.getScaledInstance(size.width, size.height, BufferedImage.SCALE_SMOOTH);

		PopupMenu menu = new PopupMenu();

		final CheckboxMenuItem start = new CheckboxMenuItem("Start on system startup", userSettings.shouldStartOnStartup());

		start.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				userSettings.setStartOnStartup(start.getState());

				try {
					saveUserSettings();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				checkStartup();
			}
		});

		menu.add(start);

		MenuItem exit = new MenuItem("Exit");

		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(1);
			}
		});

		menu.add(exit);

		try {
			SystemTray.getSystemTray().add(new TrayIcon(image, "RSLog Tracker", menu));
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Re-checks the start on startup option.
	 */
	private void checkStartup() {
		try {
			File jarFile = Util.getJarFile(ProgressTracker.class);

			logger.info("Launch location: " + jarFile.getAbsolutePath());

			if (userSettings.shouldStartOnStartup()) {
				verifyAutostart(jarFile, VerificationMode.INSERT);
			} else {
				verifyAutostart(jarFile, VerificationMode.REMOVE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Track a screenshot directory.
	 *
	 * @param dir The screenshot gallery directory.
	 * @throws IOException If an error occurs while tracking file modifications.
	 */
	public void track(File dir) throws IOException {
		logger.info("Watching " + dir.getAbsolutePath());

		watchService.execute(new DirectoryCreationWatcher(this, dir));

		for (File file : dir.listFiles()) {
			if (!file.isDirectory()) {
				continue;
			}

			trackScreenshots(file);
		}
	}

	/**
	 * Track a display name's screenshot directory.
	 *
	 * @param dir The subdirectory to track.
	 */
	public void trackScreenshots(File dir) {
		logger.info("Tracking screenshots for " + dir.getName());

		watchService.execute(new ScreenshotWatcher(this, dir));
	}

	/**
	 * Parse the file after it was modified, checking new events.
	 *
	 * @param directory The directory in which the screenshot was created.
	 * @param file The file which was modified.
	 * @throws IOException if an error occurs while reading or deserializing the file.
	 */
	public void screenshotCreated(File directory, File file) throws IOException {
		logger.info("Screenshot found for " + directory.getName() + ", file: " + file.getName());

		OSBuddyEvent evt = parseEvent(directory.getName(), file);

		if (evt == null || parsedEvents.contains(evt.hashCode())) {
			return;
		}

		parsedEvents.add(evt.hashCode());

		worker.queue(evt);
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
				.put("time", System.currentTimeMillis() / 1000L);

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
				logger.info("[" + evt.getDisplayName() + "] Skill Level - Skill : " + levelUp.getSkill() + ", Level: " + levelUp.getLevel());
				break;
			case TREASURE_TRAIL:
				TreasureTrailEvent trail = (TreasureTrailEvent) evt;

				data.put("difficulty", trail.getDifficulty());
				logger.info("[" + evt.getDisplayName() + "] Treasure Trail - Difficulty: " + trail.getDifficulty());
				break;
			case DUEL_VICTORY:
				DuelVictoryEvent victory = (DuelVictoryEvent) evt;

				data.put("opponent", victory.getOpponent());
				logger.info("[" + evt.getDisplayName() + "] Duel Victory - Opponent: " + victory.getOpponent());
				break;
			}

			try (HttpPostRequest request = new HttpPostRequest(settings.getUpdateUrl())) {
				request.setParameters(data);

				String body = request.getResponseBody();

				if (!body.equals("ok")) {
					body = body.trim();

					logger.info("Got auth token " + body);

					authStore.setAuth(evt.getDisplayName(), body);
					authStore.save();
				} else {
					logger.info("[" + evt.getDisplayName() + "] Update successfully pushed.");
				}
			}
		} catch (IOException e) {
			System.out.println("[" + evt.getDisplayName() + "] An error occurred while pushing the update.");
			e.printStackTrace();
		}
	}

	/**
	 * Parse an osbuddy gallery entry into an event.
	 *
	 * @param displayName The character display name.
	 * @param file The screenshot file.
	 * @return The parsed event, or null if unable to find one.
	 * @throws ParseEventError If an error occurs while parsing the event (invalid skill, etc)
	 */
	private OSBuddyEvent parseEvent(String displayName, File file) throws ParseEventError {
		String screenshotName = file.getName().substring(0, file.getName().lastIndexOf('.'));

		BufferedImage screenshot = null;

		// Attempt to load the screenshot

		try {
			screenshot = ImageIO.read(file);
		} catch (Exception e) {
			// Nothing to it.
			e.printStackTrace();
		}

		// Level up

		Matcher m = LEVEL_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String skill = m.group(1), level = m.group(2);

			if (!SKILLS.contains(skill)) {
				throw new ParseEventError("Unknown skill " + skill);
			}

			return new LevelUpEvent(file, displayName, screenshot, skill, Integer.parseInt(level));
		}

		// Treasure Trail

		m = TREASURE_TRAIL_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String difficulty = m.group(1);

			String timestamp = m.group(2);

			return new TreasureTrailEvent(file, displayName, screenshot, difficulty, timestamp);
		}

		// Duel victory

		/*m = DUEL_VICTORY_PATTERN.matcher(screenshotName);

		if (m.find()) {
			String opponent = m.group(1);
			String timestamp = m.group(2);

			return new DuelVictoryEvent(entry, displayName, screenshot, opponent, timestamp);
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

	/**
	 * If on windows or linux, verify that the registry entry is intact and pointing to the correct version.
	 * @param file
	 * 			The file to verify against
	 * @param mode
	 * 			The mode to use, REMOVE will remove the entry, VERIFY will update it, and INSERT will insert if it does not exist.
	 */
	public static void verifyAutostart(File file, VerificationMode mode) {
		OperatingSystem system = Util.getPlatform();
		if(system == OperatingSystem.WINDOWS) {
			try {
				String current = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, WinRegistry.RUN_PATH, APPLICATION_NAME);
				if(mode == VerificationMode.REMOVE) {
					WinRegistry.deleteValue(WinRegistry.HKEY_CURRENT_USER, WinRegistry.RUN_PATH, APPLICATION_NAME);
				} else if(mode == VerificationMode.INSERT && current == null || current != null && !current.equals(file.getAbsolutePath())) {
					WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, WinRegistry.RUN_PATH, APPLICATION_NAME, file.getAbsolutePath());
				}
			} catch (Exception e) {
				//Ignore it.
			}
		} else if(system == OperatingSystem.LINUX) {
			File autostartDir = new File(System.getenv("XDG_CONFIG_HOME") != null ? System.getenv("XDG_CONFIG_HOME") : System.getProperty("user.home") + "/.config/autostart");
			if(autostartDir.exists()) {
				File autostart = new File(autostartDir, APPLICATION_NAME.toLowerCase() + ".desktop");

				if(mode == VerificationMode.REMOVE) {
					autostart.delete();
				} else {
					String contents = new DesktopEntryBuilder()
						.addEntry("Type", "Application")
						.addEntry("Categories", "Accessories")
						.addEntry("Name", APPLICATION_NAME)
						.addEntry("Comment", APPLICATION_NAME)
						.addEntry("Terminal", false)
						.addEntry("Exec", Util.getJavaExecutable().getAbsoluteFile() + " -jar \"" + file.getAbsolutePath() + "\"")
						.build();

					try {
						FileWriter writer = new FileWriter(autostart);
						try {
							writer.write(contents);
						} finally {
							writer.close();
						}
					} catch(IOException e) {
					}
				}
			}
		}
	}

	public enum VerificationMode {
		INSERT, VERIFY, REMOVE
	}
}
