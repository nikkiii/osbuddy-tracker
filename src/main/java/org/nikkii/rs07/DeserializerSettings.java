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

	@Override
	public String toString() {
		return "DeserializerSettings [timeField=" + timeField + ", absolutePathField=" + absolutePathField + ", fileNameField=" + fileNameField + "]";
	}
}
