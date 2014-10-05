package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author Nikki
 */
public class TreasureTrailEvent extends OSBuddyEvent {
	private final String difficulty;

	public TreasureTrailEvent(String displayName, long time, BufferedImage screenshot, String difficulty) {
		super(ProgressType.TREASURE_TRAIL, displayName, time, screenshot);
		this.difficulty = difficulty;
	}

	public String getDifficulty() {
		return difficulty;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, displayName, time, difficulty);
	}
}
