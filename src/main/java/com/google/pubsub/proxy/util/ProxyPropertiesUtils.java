package com.google.pubsub.proxy.util;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * One time read of proxy.properties Read properties such as token_expiry,
 * failed message sink etc.
 */
public class ProxyPropertiesUtils {

	private static Properties prop;
	private static final Logger logger = Logger.getLogger(ProxyPropertiesUtils.class.getName());

	static {
		InputStream is = null;
		try {

			prop = new Properties();
			is = ProxyPropertiesUtils.class.getResourceAsStream("/proxy.properties");
			prop.load(is);
			is.close();

		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
	}

	public static String getPropertyValue(String key) {
		return prop.getProperty(key);
	}
}