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
 * Called when the server starts/shuts down. 
 * Any heavy lifting e.g: one time resource creation, handler initializations etc. 
 * can be done here & be saved in servletcontext for later retreival 
 */
package com.google.pubsub.proxy.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.auth.oauth2.ServiceAccountCredentials;

public class ServletInit implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(ServletInit.class.getName());

	/**
	 * On startup save service account in servlet context 
	 * @param event
	 */
	public void contextInitialized(ServletContextEvent event) {
		try {
			InputStream credsStream = new FileInputStream(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
			ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(credsStream);
			event.getServletContext().setAttribute("serviceaccount", serviceAccount);
		} 
		catch (IOException e) {
			LOGGER.severe("Init exception caught: " + e.getMessage());
		}
	}

	public void contextDestroyed(ServletContextEvent event) {}
}
