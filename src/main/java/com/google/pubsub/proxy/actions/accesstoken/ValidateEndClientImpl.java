package com.google.pubsub.proxy.actions.accesstoken;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.apache.commons.codec.binary.Base64;

import com.google.pubsub.proxy.exceptions.ClientMismatchException;

/**
 * Intercepts requests to /getaccesstoken. Basic auth checks if the request is
 * coming from registered end client (same as the one configured in proxy
 * properties)
 */
@ValidateEndClient
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ValidateEndClientImpl implements ContainerRequestFilter {

	private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";
	private static final String CLIENT_ID = "CLIENTID";

	/**
	 * Does the following: 1) Extract auth header 2) validate client_id 3) either
	 * throw an error or let the flow continue
	 */
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		// Validate the Authorization header
		if (!isTokenBasedAuthentication(authorizationHeader)) {
			throw new ClientMismatchException();
		}
		// Extract the token from the Authorization header & validate
		validateEndClient(authorizationHeader.substring(AUTHENTICATION_SCHEME_BASIC.length()).trim());
	}

	/**
	 * @param authorizationHeader
	 * @return
	 * @throws ClientMismatchException
	 */
	private boolean isTokenBasedAuthentication(String authorizationHeader) throws ClientMismatchException {
		return authorizationHeader != null
				&& authorizationHeader.toLowerCase()
				.startsWith(AUTHENTICATION_SCHEME_BASIC.toLowerCase() + " ");
	}

	/**
	 * @param token
	 * @throws ClientMismatchException
	 */
	private void validateEndClient(String token) throws ClientMismatchException {
		// clientid received in user request
		String receivedClientId = new String(Base64.decodeBase64(token.getBytes()));

		// clientid passed as a secret in environment variable during proxy startup
		String savedClientId = System.getenv(CLIENT_ID);

		// Do they match?
		if (!savedClientId.equals(receivedClientId)) {
			throw new ClientMismatchException();
		}
	}
}