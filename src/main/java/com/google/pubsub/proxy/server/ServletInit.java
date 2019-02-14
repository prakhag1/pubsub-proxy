package com.google.pubsub.proxy.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.pubsub.proxy.util.ProxyPropertiesUtils;

/**
 * Called when either the server starts up or shuts down. 
 * Any heavy lifting (e.g: one time resource creation, handler initializations etc.) 
 * can be done here & save in servletcontext for later retreival
 */
public class ServletInit implements ServletContextListener {
	
	private static final Logger LOGGER = Logger.getLogger(ServletInit.class.getName());
	
	/**
	 * On server startup, do the following:
	 * 
	 * 1) Fetch and save service account in servlet context
	 * 2) Get a bigquery instance and save it in the servletcontext -> This would be used later on to write failed messages to bq
	 * 3) Check if the dataset for bq sink already exists. If not, then create it
	 * 4) Check if the table for bq sink already exists. if not, then create it
	 */
	public void contextInitialized(ServletContextEvent event) {
				
		try 
		{	
			
			// Read service account json from k8s secret
			InputStream credsStream = new FileInputStream(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
			ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(credsStream);

			// Setservice account in context for later use
			event.getServletContext().setAttribute("serviceaccount", serviceAccount);
						
			// Get BQ handler
			BigQuery bigquery = getBQHandler(event);
			
			// Set bq handler in context for later use. Saves the hasell of dynamic object creation.
			event.getServletContext().setAttribute("bigquery", bigquery);
			
			// Create dataset if not exists
			createDataSet(bigquery);
			
			// Create table with schema if not exists
			createTable(bigquery);
			
		} 
		catch (IOException | BigQueryException e) {
			LOGGER.severe("Init exception caught: "+e.getMessage());
		}
	}
	

	/**
	 * 
	 * @param event
	 * @return
	 * @throws IOException
	 */
	private BigQuery getBQHandler(ServletContextEvent event) throws IOException {
		return BigQueryOptions.getDefaultInstance().getService();
	}
	

	/**
	 * 
	 * @param bigquery
	 */
	private void createDataSet(BigQuery bigquery) {

		String datasetName = ProxyPropertiesUtils.getPropertyValue("dataset");

		// Check if dataset exists
		Dataset dataset = bigquery.getDataset(datasetName);
		if (null == dataset) {
			DatasetInfo datasetInfo = DatasetInfo.newBuilder(datasetName).build();
			dataset = bigquery.create(datasetInfo);
		}
	}
	
	
	/**
	 * 
	 * @param bigquery
	 */
	private void createTable(BigQuery bigquery) {
		
		String tableName = ProxyPropertiesUtils.getPropertyValue("table");
		String datasetName = ProxyPropertiesUtils.getPropertyValue("dataset");

		// Check if table exists
		Table table = bigquery.getTable(datasetName, tableName);
		if (null == table) {
			
			TableId tableId = TableId.of(datasetName, tableName);
			List<Field> fields = new ArrayList<Field>();
			
			// Data: data passed in user request
			fields.add(Field.of("Data", LegacySQLTypeName.STRING));
			
			// HttpCode: Failure code returned by PubSub
			fields.add(Field.of("HttpCode", LegacySQLTypeName.INTEGER));
			
			// StatusMsg: Failure message
			fields.add(Field.of("StatusMsg", LegacySQLTypeName.STRING));
			
			// TimeStamp: Timestamp recorded at the time of record entry
			fields.add(Field.of("TimeStamp", LegacySQLTypeName.TIMESTAMP));

			Schema schema = Schema.of(fields);
			TableDefinition tableDefinition = StandardTableDefinition.of(schema);
			TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

			table = bigquery.create(tableInfo);
		}

	}
	
	public void contextDestroyed(ServletContextEvent event) {}
}
