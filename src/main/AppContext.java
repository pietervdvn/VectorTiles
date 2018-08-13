package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.LogManager;

public class AppContext {
	private Properties props;

	public AppContext(String propertiesPath) throws IOException {
		props = new Properties();
		loadProperties(propertiesPath);
	}

	/**
	 * Configures the logger.
	 * 
	 * @param path
	 *            pathname of the config file
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public static void configLogger(String path) throws SecurityException, IOException {
		try (FileInputStream f = new FileInputStream(path)) {
			Path logDir = FileSystems.getDefault().getPath("log");
			if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
				Files.createDirectory(logDir);
			}
			LogManager.getLogManager().readConfiguration(f);
		}
	}

	/**
	 * Loads properties file.
	 * 
	 * @param properties
	 *            pathname of the properties file
	 * @return
	 * @throws IOException 
	 */

	public void loadProperties(String properties) throws IOException {
		try (FileInputStream file = new FileInputStream(properties)) {
			props.load(file);
		}
	}

	/**
	 * Returns the value linked to the key in the properties file.
	 * 
	 * @param key
	 *            property name
	 * @return property value
	 */
	public String getProperty(String key) {
		String property = this.props.getProperty(key);
		if(property == null) {
			throw new NullPointerException("Missing property in the properties file: "+key);
		}
		return property;
	}
	
	public boolean getFlag(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}
	
	public int getPropertyInt(String key) {
		return Integer.parseInt(getProperty(key));
	}
}


