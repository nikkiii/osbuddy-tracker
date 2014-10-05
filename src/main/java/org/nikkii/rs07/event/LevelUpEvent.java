package org.nikkii.rs07.event;

import org.nikkii.rs07.ProgressType;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author Nikki
 */
public class LevelUpEvent extends OSBuddyEvent {
	private final String skill;
	private final int level;

	public LevelUpEvent(String displayName, long time, BufferedImage screenshot, String skill, int level) {
		super(ProgressType.LEVEL_UP, displayName, time, screenshot);
		this.skill = skill;
		this.level = level;
	}

	public String getSkill() {
		return skill;
	}

	public int getLevel() {
		return level;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, displayName, time, skill, level);
	}
}
