package org.nikkii.rs07;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Nikki
 */
public class AuthStore {
	private File dir;

	private Map<String, String> auths;

	public AuthStore(File dir) {
		this.dir = dir;
		this.auths = new HashMap<>();
	}

	public boolean hasAuth(String displayName) {
		return auths.containsKey(formatName(displayName));
	}

	public void setAuth(String displayName, String auth) {
		auths.put(formatName(displayName), auth);
	}

	public String getAuth(String displayName) {
		return auths.get(formatName(displayName));
	}

	public static AuthStore load(File dir) throws IOException {
		if (!dir.exists()) {
			dir.mkdir();
		}

		AuthStore store = new AuthStore(dir);
		for (File f : dir.listFiles()) {
			String name = f.getName().substring(0, f.getName().lastIndexOf('.'));

			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
				store.auths.put(name, reader.readLine());
			}
		}

		return store;
	}

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

	public static String formatName(String displayName) {
		displayName = displayName.toLowerCase();
		displayName = displayName.replace(' ', '_');
		return displayName;
	}
}
