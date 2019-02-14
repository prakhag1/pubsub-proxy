package com.google.pubsub.proxy.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;

public class KeyUtils {

	/**
	 * 
	 * @param keyId
	 * @param obj
	 * @throws Exception
	 */
	public static void writeTo(String keyId, JSONObject obj) throws Exception {
						
		FileOutputStream fos = new FileOutputStream(keyId + ".pub");
	    fos.write(getPublicKeyFromJSONObject(obj).getEncoded());
	    fos.close();
	}

	/**
	 * 
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public static PublicKey getPublicKeyFromJSONObject (JSONObject obj) throws Exception {
		return KeyUtils.decodePublicKey(
						pemToDer(new String(Base64.decodeBase64(((String) obj.get("publicKeyData")).getBytes()))));
	}
	
	/**
	 * 
	 * @param keyId
	 * @return
	 * @throws Exception
	 */
	public static Key readFrom(String keyId) throws Exception {

		Key pk = null;
	    File f = new File(keyId + ".pub");
	    FileInputStream fis = new FileInputStream(f);
	    DataInputStream dis = new DataInputStream(fis);
	    byte[] keyBytes = new byte[(int)f.length()];
	    dis.readFully(keyBytes);
	    dis.close();

	    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
	    KeyFactory kf = KeyFactory.getInstance("RSA");
	    pk = kf.generatePublic(spec);
	    return pk;

	}
	
	/**
	 * 
	 * @param pem
	 * @return
	 * @throws IOException
	 */
	public static byte[] pemToDer(String pem) throws IOException {
		return Base64.decodeBase64(stripBeginEnd(pem));
	}

	/**
	 * 
	 * @param pem
	 * @return
	 */
	public static String stripBeginEnd(String pem) {

		String stripped = pem.replaceAll("-----BEGIN (.*)-----", "");
		stripped = stripped.replaceAll("-----END (.*)----", "");
		stripped = stripped.replaceAll("\r\n", "");
		stripped = stripped.replaceAll("\n", "");

		return stripped.trim();
	}

	/**
	 * 
	 * @param der
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchProviderException
	 */
	public static PublicKey decodePublicKey(byte[] der)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {

		X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
		KeyFactory kf = KeyFactory.getInstance("RSA"
		// , "BC" //use provider BouncyCastle if available.
		);

		return kf.generatePublic(spec);
	}

	/**
	 * 
	 * @param keyId
	 */
	public static void cleanUpResources(String keyId) {

		File f = new File(keyId + ".pub");
		if (f.exists()) {
			f.delete();
		}
	}
}