package com.google.pubsub.proxy.exceptions;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception thrown in case of an invalid access token 
 */
@Provider
public class AccessTokenAuthException extends IOException implements ExceptionMapper<AccessTokenAuthException> {

	private static final long serialVersionUID = 1L;

	public AccessTokenAuthException() {
        super("JWT Token either expired or incorrect. You can try again by generating new token.");
    }
 
    public AccessTokenAuthException(String string) {
        super(string);
    }
    
	public Response toResponse(AccessTokenAuthException exception) {
		return Response.status(Status.UNAUTHORIZED).entity(exception.getMessage()).type(MediaType.APPLICATION_JSON).build();
	}
}
