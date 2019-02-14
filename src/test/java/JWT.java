import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
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
import com.google.pubsub.proxy.key.asymmetric.ServiceAccountJwtAccessCredentials;
import com.google.pubsub.proxy.util.ProxyPropertiesUtils;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

public class JWT {

	private GoogleCredential credential;

	private final String SIGN_JWT_ENDPOINT = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/";
	private final String PUBLIC_KEY_ENDPOINT = "https://iam.googleapis.com/v1/projects/-/serviceAccounts/";
	private final String PUBLIC_KEY_TYPE = "?publicKeyType=TYPE_RAW_PUBLIC_KEY";
	private final String CONTENT_TYPE = "text/plain";
	private final String AUTH_SCHEME = "Bearer";
	private final String AUD = "https://pubsub.googleapis.com/google.pubsub.v1.Publisher";
	private final String CLIENT_ID = "client_id";
	private final String SERVICE_ACCOUNT = "service-account";

	public static void main(String[] args) {
		ServiceAccountJwtAccessCredentials jwt = new ServiceAccountJwtAccessCredentials();
		try {
			jwt.getAccessToken();
			jwt.verifySignature("4c025428e566870d1a11159b12aea2d1cfb2d084","eyJhbGciOiJSUzI1NiIsImtpZCI6IjRjMDI1NDI4ZTU2Njg3MGQxYTExMTU5YjEyYWVhMmQxY2ZiMmQwODQiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiAicHVic3ViLXByb3h5QHZpdGFsLW9jdGFnb24tMTA5NjEyLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwgImF1ZCI6ICJodHRwczovL3B1YnN1Yi5nb29nbGVhcGlzLmNvbS9nb29nbGUucHVic3ViLnYxLlB1Ymxpc2hlciIsICJzdWIiOiAiMTIzIn0.lcJs1laZnCuZygZYhI3NjBn3rbnhGUNdu51P4otm8CifoXIvdZWGB0xl7FHqpx6YMCRDKBcyNEUoBetj2t1vjA-GONg6ePP1Vpk2o6X_p5XSYTq1u_fModEXsazdbdJ5iQNQ7YEb79WhFoa5IpPyOwB-GZeiYSQOwRt830PB6qf9ssK7eIYNSveyHMr3pFBpvkdg-dzPIlGLypIwYdnhE49MQ3yj2AGAYnsUYczG8VMBuZlsnvZMDR6cYr2Hm04BKQ-cq8xrkqwHZKMVqMNxZVemOq_e52UDUy3ISdNOKKVKBLcUoSTG82FTYr-EsFuYnuFvr8_qUT3gtLm2XIT5-g");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getAccessToken() throws Exception {

		long startTime = System.currentTimeMillis();

		// Get service account credentials
		setCredentials();

		// Prepare unsigned JWT
		Map<String, String> token = prepareJWTPayload();

		// Sign JWT with service account
		JSONObject response = signJwtWithServiceAccount(token);

		// Async - Store public key locally for later retreival
		saveKey((String) response.get("keyId"), Optional.empty());

		long endTime = System.currentTimeMillis();
		System.out.println("Time elapsed getToken: " + (endTime - startTime));
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
				+ "\"sub\": \"" + ProxyPropertiesUtils.getPropertyValue(CLIENT_ID) + "\"}");

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
	 * @param object
	 * @throws Exception
	 */
	private void saveKey(String keyId, Optional<JSONObject> obj) throws Exception {

		// If already present, do nothing
		File f = new File(keyId + ".pub");
		if (f.exists())
			return;

		// Fork a background process
		else {
			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						// Write key to file
						writeTo(keyId, obj.isPresent() ? obj.get() : getKeyFromAPI(keyId));
					} catch (Exception ex) {
						System.out.println(ex.getMessage());
					}
				}
			}).start();
		}
	}

	/**
	 * 
	 * @param keyId
	 * @param signedJwt
	 * @throws Exception
	 */
	public void verifySignature(String keyId, String signedJwt) throws Exception {

		long startTime = System.currentTimeMillis();

		// Get service account credentials
		setCredentials();

		// Get public key details
		JSONObject keyDetails = new File(keyId + ".pub").exists() ? getSavedKey(keyId) : getKeyFromAPI(keyId);

		// Convert received string to PublicKey
		PublicKey publicKey = decodePublicKey(
				pemToDer(new String(Base64.decodeBase64(((String) keyDetails.get("publicKeyData")).getBytes()))));

		try {
			// Verify token & claims
			Jwts.parser().setSigningKey(publicKey).parseClaimsJws(signedJwt);
		} 
		catch (ClaimJwtException | SignatureException e) {
			// If there's key expiration/corruption, clean up locally cached key
			cleanUpResources(keyId);
		}

		long endTime = System.currentTimeMillis();

		System.out.println("Time elapsed verify: " + (endTime - startTime));

	}

	/**
	 * 
	 * @param keyId
	 * @param obj
	 * @throws Exception
	 */
	private void writeTo(String keyId, JSONObject obj) throws Exception {

		File f = new File(keyId + ".pub");
		FileWriter file = new FileWriter(f);
		file.write(obj.toJSONString());
		file.close();
	}

	/**
	 * 
	 * @param keyId
	 * @return
	 * @throws Exception
	 */
	private JSONObject getSavedKey(String keyId) throws Exception {

		InputStream is = new FileInputStream(keyId + ".pub");
		String jsonTxt = IOUtils.toString(is, "UTF-8");

		JSONParser parser = new JSONParser();
		return ((JSONObject) parser.parse(jsonTxt));

	}

	/**
	 * 
	 * @param keyId
	 * @return
	 * @throws Exception
	 */
	private JSONObject getKeyFromAPI(String keyId) throws Exception {

		final String serviceAccount = System.getProperty(SERVICE_ACCOUNT);

		final HttpRequest request = new NetHttpTransport().createRequestFactory().buildGetRequest(
				new GenericUrl(PUBLIC_KEY_ENDPOINT + serviceAccount + "/keys/" + keyId + PUBLIC_KEY_TYPE));

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
	 * @param pem
	 * @return
	 * @throws IOException
	 */
	private byte[] pemToDer(String pem) throws IOException {
		return Base64.decodeBase64(stripBeginEnd(pem));
	}

	/**
	 * 
	 * @param pem
	 * @return
	 */
	private String stripBeginEnd(String pem) {

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
	private PublicKey decodePublicKey(byte[] der)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {

		X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
		KeyFactory kf = KeyFactory.getInstance("RSA"
		// , "BC" //use provider BouncyCastle if available.
		);

		return kf.generatePublic(spec);
	}

	private void cleanUpResources(String keyId) {

		File f = new File(keyId + ".pub");
		if (f.exists()) {
			f.delete();
		}
	}

	private void setCredentials() throws Exception {
		if (null == credential) {
			credential = GoogleCredential.getApplicationDefault(Utils.getDefaultTransport(),
					Utils.getDefaultJsonFactory());

			// GoogleCredentials credentials = ComputeEngineCredentials.create();
			// System.out.println(credentials.getAccessToken());
		}
	}
}
