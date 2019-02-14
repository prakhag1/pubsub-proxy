package com.google.pubsub.proxy.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception thrown in case mandatory fileds (i.e. topic and/or messages is missing) 
 */
@Provider
public class MissingRequiredFieldsException extends Exception implements ExceptionMapper<MissingRequiredFieldsException>{

	private static final long serialVersionUID = 1L;

	public MissingRequiredFieldsException() {
        super("Required parameter(s) missing in request.");
    }
 
    public MissingRequiredFieldsException(String string) {
        super(string);
    }
    
	public Response toResponse(MissingRequiredFieldsException exception) {
		return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).type(MediaType.APPLICATION_JSON).build();
	}
}
