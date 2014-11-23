package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;
import org.nikkii.rs07.gallery.GalleryEntry;
import org.nikkii.rs07.util.Optional;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author Nikki
 */
public class OSBuddyEvent {
	protected final GalleryEntry entry;
	protected final ProgressType type;
	protected final String displayName;
	protected Optional<BufferedImage> screenshot;

	public OSBuddyEvent(GalleryEntry entry, ProgressType type, String displayName, BufferedImage screenshot) {
		this.entry = entry;
		this.type = type;
		this.displayName = displayName;
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

	public void setScreenshot(BufferedImage screenshot) {
		this.screenshot = Optional.ofNullable(screenshot);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, displayName, entry.getTime());
	}

	public GalleryEntry getEntry() {
		return entry;
	}
}
