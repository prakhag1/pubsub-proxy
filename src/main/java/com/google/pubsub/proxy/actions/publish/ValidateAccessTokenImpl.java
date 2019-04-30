/* Copyright 2019 Google Inc. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License. */

package com.google.pubsub.proxy.actions.publish;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

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

@Provider
@ValidateAccessToken
@Priority(Priorities.AUTHENTICATION)
public class ValidateAccessTokenImpl implements ContainerRequestFilter {
	@Context
	ServletContext ctx;
	private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";
	private static final Logger LOGGER = Logger.getLogger(ValidateAccessTokenImpl.class.getName());

	
	/**
	 * 1) Extracts bearer token from user request
	 * 2) Validates JWT signature using service account from environment variable (set via k8s secret)
	 * @param requestContext - user request
	 */
	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {
			String token = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION).substring(AUTHENTICATION_SCHEME_BEARER.length()).trim();
			LOGGER.info("Token: "+ token);
			ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) ctx.getAttribute("serviceaccount");
			Jwts.parser().setSigningKey(serviceAccount.getPrivateKey()).parseClaimsJws(token);
			LOGGER.info("matched token");
		} catch (Exception e) {
			if (!(e instanceof NoSuchAlgorithmException)) {
				throw new AccessTokenAuthException();
			}
		}
	}
}