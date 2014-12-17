package org.nikkii.rs07.util;

import java.io.File;

/**
 * Contains basic platform and jar utils.
 *
 * @author Nikki
 */
public class Util {
	/**
	 * An enum containing operating system types
	 *
	 * @author Nikki
	 *
	 */
	public static enum OperatingSystem {
		LINUX, SOLARIS, WINDOWS, MAC, UNKNOWN
	}

	/**
	 * The cached operating system
	 */
	public static final OperatingSystem SYSTEM = getPlatform();

	/**
	 * Get the current platform
	 *
	 * @return The current platform
	 */
	public static OperatingSystem getPlatform() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win"))
			return OperatingSystem.WINDOWS;
		if (osName.contains("mac"))
			return OperatingSystem.MAC;
		if (osName.contains("solaris"))
			return OperatingSystem.SOLARIS;
		if (osName.contains("sunos"))
			return OperatingSystem.SOLARIS;
		if (osName.contains("linux"))
			return OperatingSystem.LINUX;
		if (osName.contains("unix"))
			return OperatingSystem.LINUX;
		return OperatingSystem.UNKNOWN;
	}

	/**
	 * Find the java executable for the current java runtime
	 *
	 * @return
	 * 		The File of the java executable, or null if it was not found (This is very bad)
	 */
	public static File getJavaExecutable() {
		File javaDir = new File(System.getProperty("java.home"));

		String[] exes = new String[] { "bin/javaw", "bin/java" };

		boolean isWindows = SYSTEM == OperatingSystem.WINDOWS;

		for(String s : exes) {
			if(isWindows) {
				s = s + ".exe";
			}
			File f = new File(javaDir, s);
			if(f.exists()) {
				return f;
			}
		}
		return null;
	}

	/**
	 * Get the jar running from as a File object
	 *
	 * @param cl
	 * 			The class to get the path from
	 * @return
	 * 			A File object representing the path
	 * @throws Exception
	 * 			If the path is invalid
	 */
	public static File getJarFile(Class<?> cl) throws Exception {
		return new File(cl.getProtectionDomain().getCodeSource()
			.getLocation().toURI());
	}
}
