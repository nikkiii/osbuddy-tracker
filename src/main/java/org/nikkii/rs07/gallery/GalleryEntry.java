package org.nikkii.rs07.gallery;

/**
 * @author Nikki
 */
public class GalleryEntry {
	private long time;

	private String absolutePath;

	private String fileName;

	public GalleryEntry(long time, String absolutePath, String fileName) {
		this.time = time;
		this.absolutePath = absolutePath;
		this.fileName = fileName;
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

	@Override
	public String toString() {
		return "GalleryEntry [time=" + time + ", absolutePath=" + absolutePath + ", fileName=" + fileName + "]";
	}
}
