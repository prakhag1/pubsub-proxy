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
 * Embedded jetty with config:
 * Port:8080 
 * Resources: Endpoints (/health, /publish) 
 * Providers: Validation filters and exceptions (refer InjectResourcesUtils for details)
 */
package com.google.pubsub.proxy.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.pubsub.proxy.util.InjectResourcesUtils;

public class WebServer {
	public static void main(String[] args) throws Exception {
		ServletContextHandler contextHandler = new ServletContextHandler();
		
		ServletHolder servletHolder = new ServletHolder(new ServletContainer(InjectResourcesUtils.injectResources()));
		contextHandler.addServlet(servletHolder, "/*");
		contextHandler.addEventListener(new ServletInit());

		Server server = new Server(8080);
		server.setHandler(contextHandler);
		server.start();
		server.join();
	}
}