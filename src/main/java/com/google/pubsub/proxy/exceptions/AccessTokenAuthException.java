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

/*
 * Exception thrown in case of an invalid access token 
 */
package com.google.pubsub.proxy.exceptions;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AccessTokenAuthException extends IOException implements ExceptionMapper<AccessTokenAuthException> {
	private static final long serialVersionUID = 1L;

	public AccessTokenAuthException() {
        super("JWT Token either expired or incorrect. You can try again by generating new token.");
    }
 
    public AccessTokenAuthException(String string) {
        super(string);
    }
    
    public AccessTokenAuthException(Exception e) {
        super(e);
    }
    
	public Response toResponse(AccessTokenAuthException exception) {
		return Response.status(Status.UNAUTHORIZED)
				.entity(exception.getMessage())
				.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
