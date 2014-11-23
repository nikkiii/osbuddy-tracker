package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;
import org.nikkii.rs07.gallery.GalleryEntry;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author Nikki
 */
public class DuelVictoryEvent extends OSBuddyEvent {
	private final String opponent;

	public DuelVictoryEvent(GalleryEntry entry, String displayName, BufferedImage screenshot, String opponent) {
		super(entry, ProgressType.DUEL_VICTORY, displayName, screenshot);
		this.opponent = opponent;
	}

	public String getOpponent() {
		return opponent;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entry.getTime(), type, displayName, entry, opponent);
	}
}
