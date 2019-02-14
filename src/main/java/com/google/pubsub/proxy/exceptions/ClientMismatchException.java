package com.google.pubsub.proxy.exceptions;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception thrown in case client id configured in proxy.properties 
 * doesn't match with what's passed in user request (/getaccesstoken)
 */
@Provider
public class ClientMismatchException extends IOException implements ExceptionMapper<ClientMismatchException> {

	private static final long serialVersionUID = 1L;

	public ClientMismatchException() {
        super("Missing or incorrect client id");
    }
 
    public ClientMismatchException(String string) {
        super(string);
    }
    
	public Response toResponse(ClientMismatchException exception) {
		return Response.status(Status.UNAUTHORIZED).entity(exception.getMessage()).type(MediaType.APPLICATION_JSON).build();
	}
}
