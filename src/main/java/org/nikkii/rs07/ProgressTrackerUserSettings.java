package org.nikkii.rs07;

/**
 * Contains basic user tracker settings.
 *
 * @author Nikki
 */
public class ProgressTrackerUserSettings {
	/**
	 * The update endpoint.
	 */
	private boolean startOnStartup;

	/**
	 * Sets the start on startup flag.
	 * @param startOnStartup The start on startup flag.
	 */
	public void setStartOnStartup(boolean startOnStartup) {
		this.startOnStartup = startOnStartup;
	}

	/**
	 * Gets the start on startup flag.
	 * @return The start on startup flag.
	 */
	public boolean shouldStartOnStartup() {
		return startOnStartup;
	}
}
