package com.google.pubsub.proxy.actions.publish;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.pubsub.proxy.exceptions.AccessTokenAuthException;

import io.jsonwebtoken.Jwts;

/**
 * This class intercepts requests to /publish JWT auth checks if the request
 * uses the access token that was generated & previously granted to the end
 * client (through /getaccesstoken)
 */
@Provider
@ValidateAccessToken
@Priority(Priorities.AUTHENTICATION)
public class ValidateAccessTokenImpl implements ContainerRequestFilter {

	@Context
	ServletContext ctx;

	private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";

	/**
	 * Extract the Bearer token and do cross validation/verification. A symmetric
	 * key is used to sign the JWT token. Further control on access token (expiry
	 * etc) can be set via config parameters in proxy.properties
	 */
	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {

			String token = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
					.substring(AUTHENTICATION_SCHEME_BEARER.length()).trim();

			// Get service account handler from servletcontext
			ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) ctx.getAttribute("serviceaccount");
			Jwts.parser().setSigningKey(serviceAccount.getPrivateKey()).parseClaimsJws(token);

		} catch (Exception e) {
			if (!(e instanceof NoSuchAlgorithmException)) {
				throw new AccessTokenAuthException();
			}
		}
	}
}