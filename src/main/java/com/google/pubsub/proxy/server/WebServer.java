package com.google.pubsub.proxy.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.pubsub.proxy.util.InjectResourcesUtils;

/**
 * Embedded jetty with config: Port:8080 Resources: Endpoints (/getaccesstoken,
 * /publish) Providers: Validation filters and exceptions Refer
 * InjectResourcesUtils for details
 */
public class WebServer {

	public static void main(String[] args) throws Exception {

		ServletContextHandler contextHandler = new ServletContextHandler();

		// Inject all resources & providers here
		ServletHolder servletHolder = new ServletHolder(new ServletContainer(InjectResourcesUtils.injectResources()));
		contextHandler.addServlet(servletHolder, "/*");
		contextHandler.addEventListener(new ServletInit());

		// Start server
		Server server = new Server(8080);
		server.setHandler(contextHandler);
		server.start();
		server.join();
	}
}