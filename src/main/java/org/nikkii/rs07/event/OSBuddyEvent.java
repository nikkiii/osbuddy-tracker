package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Nikki
 */
public class OSBuddyEvent {
	protected final ProgressType type;
	protected final String displayName;
	protected final long time;
	private final Optional<BufferedImage> screenshot;

	public OSBuddyEvent(ProgressType type, String displayName, long time, BufferedImage screenshot) {
		this.type = type;
		this.displayName = displayName;
		this.time = time;
		this.screenshot = Optional.ofNullable(screenshot);
	}

	public ProgressType getType() {
		return type;
	}

	public String getDisplayName() {
		return displayName;
	}

	public BufferedImage getScreenshot() {
		return screenshot.get();
	}

	public boolean hasScreenshot() {
		return screenshot.isPresent();
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, displayName, time);
	}
}
