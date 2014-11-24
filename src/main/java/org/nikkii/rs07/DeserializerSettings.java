package org.nikkii.rs07;

/**
 * A class containing deserializer fields.
 *
 * @author Nikki
 */
public class DeserializerSettings {

	/**
	 * The field/variable name for the timestamp.
	 */
	private String timeField;

	/**
	 * The field/variable name for the absolute path.
	 */
	private String absolutePathField;

	/**
	 * The field/variable name for the filename.
	 */
	private String fileNameField;

	/**
	 * The field/variable name for the imgur deletion key.
	 */
	private String deletionKeyField;

	/**
	 * The field/variable name for the url.
	 */
	private String urlField;

	/**
	 * Gets the time field name.
	 *
	 * @return The time field name.
	 */
	public String getTimeField() {
		return timeField;
	}

	/**
	 * Gets the absolute path field name.
	 *
	 * @return The absolute path field name.
	 */
	public String getAbsolutePathField() {
		return absolutePathField;
	}

	/**
	 * Gets the filename field name.
	 *
	 * @return The filename field name.
	 */
	public String getFileNameField() {
		return fileNameField;
	}

	/**
	 * Gets the identifier field name.
	 *
	 * @return The identifier field name.
	 */
	public String getDeletionKeyField() {
		return deletionKeyField;
	}

	/**
	 * Gets the url field name.
	 *
	 * @return The url field name.
	 */
	public String getUrlField() {
		return urlField;
	}

	@Override
	public String toString() {
		return "DeserializerSettings [timeField=" + timeField + ", absolutePathField=" + absolutePathField + ", fileNameField=" + fileNameField + ", deletionKeyField=" + deletionKeyField + ", urlField=" + urlField + "]";
	}
}
