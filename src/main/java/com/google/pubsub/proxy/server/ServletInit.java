package com.google.pubsub.proxy.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQueryException;

/**
 * Called when either the server starts up or shuts down. Any heavy lifting
 * (e.g: one time resource creation, handler initializations etc.) can be done
 * here & save in servletcontext for later retreival
 */
public class ServletInit implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(ServletInit.class.getName());

	/**
	 * On server startup, do the following:
	 * 
	 * 1) Fetch and save service account in servlet context 2) Get a bigquery
	 * instance and save it in the servletcontext -> This would be used later on to
	 * write failed messages to bq 3) Check if the dataset for bq sink already
	 * exists. If not, then create it 4) Check if the table for bq sink already
	 * exists. if not, then create it
	 */
	public void contextInitialized(ServletContextEvent event) {
		try {
			// Read service account json from k8s secret
			InputStream credsStream = new FileInputStream(/*System.getenv("GOOGLE_APPLICATION_CREDENTIALS")*/"/Users/prakhargautam/Downloads/sa.json");
			ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(credsStream);

			// Setservice account in context for later use
			event.getServletContext().setAttribute("serviceaccount", serviceAccount);
		} 
		catch (IOException | BigQueryException e) {
			LOGGER.severe("Init exception caught: " + e.getMessage());
		}
	}

	public void contextDestroyed(ServletContextEvent event) {}
}
