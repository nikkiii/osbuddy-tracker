package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

/**
 * @author Nikki
 */
public class TreasureTrailEvent extends OSBuddyEvent {
	private final String difficulty;
	private final String completionDate;

	public TreasureTrailEvent(File screenshotFile, String displayName, BufferedImage screenshot, String difficulty, String completionDate) {
		super(screenshotFile, ProgressType.TREASURE_TRAIL, displayName, screenshot);
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
