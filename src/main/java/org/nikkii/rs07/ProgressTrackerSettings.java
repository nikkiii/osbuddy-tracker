package org.nikkii.rs07;

/**
 * Contains basic tracker settings.
 *
 * @author Nikki
 */
public class ProgressTrackerSettings {
	/**
	 * The update endpoint.
	 */
	private String updateUrl;

	/**
	 * The image endpoint.
	 */
	private String imageUrl;

	/**
	 * Get the update service url.
	 *
	 * @return The update url.
	 */
	public String getUpdateUrl() {
		return updateUrl;
	}

	/**
	 * Get the image upload url.
	 *
	 * @return The image upload url.
	 */
	public String getImageUrl() {
		return imageUrl;
	}
}
