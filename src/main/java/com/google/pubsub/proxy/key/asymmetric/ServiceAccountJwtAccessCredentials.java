package com.google.pubsub.proxy.key.asymmetric;

import java.io.File;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.pubsub.proxy.util.KeyUtils;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

/**
 * This is an experimental class (not tested thoroughly) that allows developers to sign their access token using a GCP service account
 * The implementation below is quite coarse - uses API endpoints. The recommended way would be make use of relevant client libs
 * 
 * The flow goes something like this: 
 * 
 * 1) Generate a JWT
 * 2) Sign it with the private key of service account
 * 3) For verification, the JWT token requires the public key of the service account. 
 * During step 2, public key file is locally stored for later reterivals.
 */
public class ServiceAccountJwtAccessCredentials {

	private GoogleCredential credential;

	private static final String SIGN_JWT_ENDPOINT = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/";
	private static final String KEY_ENDPOINT = "https://iam.googleapis.com/v1/projects/-/serviceAccounts/";
	private static final String KEY_TYPE = "?publicKeyType=TYPE_RAW_PUBLIC_KEY";
	private static final String CONTENT_TYPE = "text/plain";
	private static final String AUTH_SCHEME = "Bearer";
	private static final String AUD = "https://pubsub.googleapis.com/google.pubsub.v1.Publisher";
	private static final String CLIENT_ID = "clientid";
	private static final String SERVICE_ACCOUNT = "service-account";

	/**
	 * 
	 * @throws Exception
	 */
	public void getAccessToken() throws Exception {

		// Get service account credentials
		setCredentials();

		// Prepare unsigned JWT
		Map<String, String> token = prepareJWTPayload();

		// Sign JWT with service account
		JSONObject response = signJwtWithServiceAccount(token);

		// Async - Store public key locally for later retreival
		saveKey((String) response.get("keyId"), Optional.empty());
		
		return;
	}

	/**
	 * Prepare unsigned JWT payload
	 * 
	 * @return
	 */
	private Map<String, String> prepareJWTPayload() {

		Map<String, String> json = new HashMap<String, String>();
		json.put("payload", "{\"iss\": \"" + System.getProperty(SERVICE_ACCOUNT) + "\", " + "\"aud\": \"" + AUD + "\", "
				+ "\"sub\": \"" + System.getenv(CLIENT_ID) + "\"}");

		return json;
	}

	/**
	 * 
	 * @param token
	 * @return
	 * @throws Exception
	 */
	private JSONObject signJwtWithServiceAccount(Map<String, String> token) throws Exception {

		final String serviceAccount = System.getProperty(SERVICE_ACCOUNT);
		final HttpContent content = new JsonHttpContent(new JacksonFactory(), token);

		final HttpRequest request = new NetHttpTransport().createRequestFactory()
				.buildPostRequest(new GenericUrl(SIGN_JWT_ENDPOINT + serviceAccount + ":signJwt"), content);

		HttpHeaders headers = request.getHeaders();
		headers.setAuthorization(AUTH_SCHEME + " " + credential.getAccessToken());
		headers.setContentType(CONTENT_TYPE);

		JSONParser parser = new JSONParser();
		return ((JSONObject) parser.parse(request.execute().parseAsString().trim()));
	}

	
	/**
	 * 
	 * @param keyId
	 * @param signedJwt
	 * @throws Exception
	 */
	public void verifySignature(String keyId, String signedJwt) throws Exception {

		Key publicKey;

		// Set service account credentials
		setCredentials();

		// Get public key 
		if(new File(keyId + ".pub").exists()) 
		{
			publicKey = KeyUtils.readFrom(keyId);
		}
		else 
		{
			JSONObject keyDetails = getKey(keyId);
			publicKey = KeyUtils.getPublicKeyFromJSONObject(keyDetails);
		}

		try {
			// Verify token & claims
			Jwts.parser().setSigningKey(publicKey).parseClaimsJws(signedJwt);
		} 
		catch (ClaimJwtException | SignatureException e) {
			// If there's key expiration/corruption, clean up locally cached key
			KeyUtils.cleanUpResources(keyId);
		}

	}

	/**
	 * 
	 * @param keyId
	 * @return
	 * @throws Exception
	 */
	private JSONObject getKey(String keyId) throws Exception {

		final String serviceAccount = System.getProperty(SERVICE_ACCOUNT);

		final HttpRequest request = new NetHttpTransport().createRequestFactory().buildGetRequest(
				new GenericUrl(KEY_ENDPOINT + serviceAccount + "/keys/" + keyId + KEY_TYPE));

		HttpHeaders headers = request.getHeaders();
		headers.setAuthorization(AUTH_SCHEME + " " + credential.getAccessToken());

		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(request.execute().parseAsString().trim());

		// Async - Store public key locally for later retreival
		saveKey(keyId, Optional.of(obj));

		return obj;

	}

	/**
	 * 
	 * @param object
	 * @throws Exception
	 */
	private void saveKey(String keyId, Optional<JSONObject> obj) throws Exception {

		// If already present, do nothing
		File f = new File(keyId + ".pub");
		if (f.exists())
			return;

		// Fork a background task
		else {
			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						// Write key to file
						KeyUtils.writeTo(keyId, obj.isPresent() ? obj.get() : getKey(keyId));
					} catch (Exception ex) {
						System.out.println(ex.getMessage());
					}
				}
			}).start();
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void setCredentials() throws Exception {
		if (null == credential) {
			credential = GoogleCredential.getApplicationDefault(Utils.getDefaultTransport(),
					Utils.getDefaultJsonFactory());
		}
	}
}
