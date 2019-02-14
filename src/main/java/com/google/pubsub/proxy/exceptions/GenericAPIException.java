package com.google.pubsub.proxy.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Generic exception wrapped as http 500 response  
 */
@Provider
public class GenericAPIException extends Exception implements ExceptionMapper<GenericAPIException>{

	private static final long serialVersionUID = 1L;

	public GenericAPIException() {
        super();
    }
 
    public GenericAPIException(String string) {
        super(string);
    }
    
    public GenericAPIException(Throwable cause) {
        super(cause);
    }
    
	public Response toResponse(GenericAPIException exception) {
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).type(MediaType.APPLICATION_JSON).build();
	}
}