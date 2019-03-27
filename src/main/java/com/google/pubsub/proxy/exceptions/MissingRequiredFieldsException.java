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
