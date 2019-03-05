package com.google.pubsub.proxy.actions.accesstoken;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.pubsub.proxy.exceptions.GenericAPIException;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * This class is used to generate an access token necessary to authenticate
 * requests to Google Cloud PubSub. Client information is provided under
 * "Authorization: base64(<client_id>)"
 */
@Path("/getaccesstoken")
public class GetAccessToken {
	@Context
	ServletContext ctx;
	/**
	 * The primary thing of interest here is the client_id coming under
	 * Authorization header. If it matches with what's provided in proxy.properties
	 * then proceed to generate a JWT token
	 */
	@GET
	@ValidateEndClient
	public Response doPost(@Context HttpHeaders hh) throws Exception {
		try {
			Map<String, String> entity = new HashMap<String, String>();
			String token = issueToken(hh.getHeaderString(HttpHeaders.AUTHORIZATION));
			entity.put("token", token);

			return Response.ok().entity(entity).type(MediaType.APPLICATION_JSON).build();
		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
	}

	/**
	 * @param header
	 * @return
	 * @throws Exception
	 */
	private String issueToken(String header) throws Exception {
		// Get service account handler from servletcontext
		ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) ctx.getAttribute("serviceaccount");
		JwtBuilder jwts = Jwts.builder();

		// Set header
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("type", "JWT");
		map.put("alg", "RS256");
		jwts.setHeader(map);

		// Set claims
		Map<String, Object> claims = new HashMap<String, Object>();
		claims.put("sub", serviceAccount.getClientEmail());
		claims.put("exp", System.currentTimeMillis() / 1000 + 3600);
		claims.put("iat", System.currentTimeMillis() / 1000);
		claims.put("iss", serviceAccount.getClientEmail());
		jwts.setClaims(claims);

		// Sign with key
		jwts.signWith(SignatureAlgorithm.RS256, serviceAccount.getPrivateKey());
		return jwts.compact();
	}
}
