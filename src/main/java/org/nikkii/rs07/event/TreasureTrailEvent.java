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
	private final String completionDate;

	public TreasureTrailEvent(GalleryEntry entry, String displayName,BufferedImage screenshot, String difficulty, String completionDate) {
		super(entry, ProgressType.TREASURE_TRAIL, displayName, screenshot);
		this.difficulty = difficulty;
		this.completionDate = completionDate;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public String getCompletionDate() {
		return completionDate;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, displayName, difficulty, completionDate);
	}
}
