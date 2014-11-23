package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;
import org.nikkii.rs07.gallery.GalleryEntry;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author Nikki
 */
public class TreasureTrailEvent extends OSBuddyEvent {
	private final String difficulty;

	public TreasureTrailEvent(GalleryEntry entry, String displayName,BufferedImage screenshot, String difficulty) {
		super(entry, ProgressType.TREASURE_TRAIL, displayName, screenshot);
		this.difficulty = difficulty;
	}

	public String getDifficulty() {
		return difficulty;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entry.getTime(), type, displayName, difficulty);
	}
}
