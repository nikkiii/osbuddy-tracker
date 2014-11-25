package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

/**
 * @author Nikki
 */
public class DuelVictoryEvent extends OSBuddyEvent {
	private final String opponent;
	private final String timestamp;

	public DuelVictoryEvent(File screenshotFile, String displayName, BufferedImage screenshot, String opponent, String timestamp) {
		super(screenshotFile, ProgressType.DUEL_VICTORY, displayName, screenshot);
		this.opponent = opponent;
		this.timestamp = timestamp;
	}

	public String getOpponent() {
		return opponent;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, displayName, opponent, timestamp);
	}
}
