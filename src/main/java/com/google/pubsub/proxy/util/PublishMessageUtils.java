package com.google.pubsub.proxy.util;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import com.google.pubsub.proxy.exceptions.GenericAPIException;

/**
 * Utils to assist methods in /publish endpoint
 */
public class PublishMessageUtils {
	
	/**
	 * @param s
	 * @return
	 * @throws GenericAPIException
	 */
	public static Timestamp getTimeStamp(String s) throws GenericAPIException {
		try {
			return Timestamps.parse(s);
		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
	}

	/**
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getAllAttributes(Object attributes) throws GenericAPIException {
		try {
			return (LinkedHashMap<String, String>) attributes;
		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
	}
}
