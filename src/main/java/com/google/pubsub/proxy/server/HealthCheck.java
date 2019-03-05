package com.google.pubsub.proxy.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
public class HealthCheck {
	/**
	 * Health check end-point. Returns a 200OK.
	 */
	@GET
	public Response doGet() {
		return Response.status(Status.OK).build();
	}
}
