package org.nikkii.rs07.gallery;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Represents an OSBuddy Gallery entry.
 *
 * @author Nikki
 */
public class GalleryEntry {
	/**
	 * The entry time.
	 */
	private final long time;

	/**
	 * The entry's absolute file path.
	 */
	private final String absolutePath;

	/**
	 * The entry's file name.
	 */
	private final String fileName;

	/**
	 * The entry's imgur deletion key.
	 */
	private final String deletionKey;

	/**
	 * The entry's screenshot url.
	 */
	private final String url;

	/**
	 * Construct a new GalleryEntry.
	 *
	 * @param time The entry time.
	 * @param absolutePath The entry path.
	 * @param fileName The entry file name.
	 */
	public GalleryEntry(long time, String absolutePath, String fileName, String deletionKey, String url) {
		this.time = time;
		this.absolutePath = absolutePath;
		this.fileName = fileName;
		this.deletionKey = deletionKey;
		this.url = url;
	}

	public long getTime() {
		return time;
	}

	public String getFileName() {
		return fileName;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getDeletionKey() {
		return deletionKey;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Attempt to read the screenshot from the absolute path.
	 *
	 * @return The screenshot.
	 * @throws IOException If an error occurs while attempting to load.
	 */
	public BufferedImage getScreenshot() throws IOException {
		return ImageIO.read(new File(absolutePath));
	}

	@Override
	public String toString() {
		return "GalleryEntry [time=" + time + ", absolutePath=" + absolutePath + ", fileName=" + fileName + ", deletionKey=" + deletionKey + ", url=" + url + "]";
	}
}
