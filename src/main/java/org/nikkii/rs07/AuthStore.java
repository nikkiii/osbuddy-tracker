package org.nikkii.rs07;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class to store the web service auth tokens.
 *
 * @author Nikki
 */
public class AuthStore {
	/**
	 * The directory which contains the auth token files.
	 *
	 * This is a directory instead of a single file to allow easy copying of tokens.
	 */
	private File dir;

	/**
	 * The auth map.
	 */
	private final Map<String, String> auths;

	/**
	 * Construct a new AuthStore.
	 *
	 * @param dir The base directory.
	 */
	public AuthStore(File dir) {
		this.dir = dir;
		this.auths = new HashMap<>();
	}

	/**
	 * Check if we have an auth token for the specified name.
	 *
	 * @param displayName The display name.
	 * @return If we have an auth token.
	 */
	public boolean hasAuth(String displayName) {
		return auths.containsKey(formatName(displayName));
	}

	/**
	 * Set an auth token.
	 *
	 * @param displayName The display name.
	 * @param auth The auth token.
	 */
	public void setAuth(String displayName, String auth) {
		auths.put(formatName(displayName), auth);
	}

	/**
	 * Get an auth token.
	 *
	 * @param displayName The display name.
	 * @return The auth token for the specified name.
	 */
	public String getAuth(String displayName) {
		return auths.get(formatName(displayName));
	}

	/**
	 * Save the auth tokens to file.
	 *
	 * @throws IOException If an error occurs while writing the tokens.
	 */
	public void save() throws IOException {
		if (!dir.exists()) {
			dir.mkdir();
		}
		for (Entry<String, String> auth : auths.entrySet()) {
			File file = new File(dir, auth.getKey() + ".txt");

			try (Writer writer = new FileWriter(file)) {
				writer.write(auth.getValue());
			}
		}
	}

	/**
	 * Format the display names to a standard name.
	 *
	 * Something Cool -> something_cool
	 *
	 * @param displayName The display name.
	 * @return The formatted display name.
	 */
	public static String formatName(String displayName) {
		displayName = displayName.toLowerCase();
		displayName = displayName.replace(' ', '_');
		return displayName;
	}

	/**
	 * Load an AuthStore from a directory.
	 *
	 * @param dir The base directory.
	 * @return The auth store instance.
	 * @throws IOException If an error occurs while reading an auth token file.
	 */
	public static AuthStore load(File dir) throws IOException {
		if (!dir.exists()) {
			dir.mkdir();
		}

		AuthStore store = new AuthStore(dir);
		for (File f : dir.listFiles()) {
			if (!f.getName().endsWith(".txt")) {
				continue;
			}

			String name = f.getName().substring(0, f.getName().lastIndexOf('.'));

			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
				store.auths.put(name, reader.readLine());
			}
		}

		return store;
	}
}
