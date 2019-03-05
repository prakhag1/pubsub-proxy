package com.google.pubsub.proxy.util;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import com.google.api.client.util.DateTime;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.TableId;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import com.google.pubsub.proxy.entities.Message;
import com.google.pubsub.proxy.exceptions.GenericAPIException;

/**
 * Utils to assist methods in /publish endpoint
 */
public class PublishMessageUtils {
	private final static String bigqueryInstance = "bigquery";
	private final static String data = "Data";
	private final static String timestamp = "TimeStamp";
	private final static String code = "HttpCode";
	private final static String statusmsg = "StatusMsg";
	/**
	 * @param s
	 * @return
	 * @throws GenericAPIException
	 */
	public static Timestamp getTimeStamp(String s) throws GenericAPIException {
		try {
			return Timestamps.parse(s);
		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
	}

	/**
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getAllAttributes(Object attributes) throws GenericAPIException {
		try {
			return (LinkedHashMap<String, String>) attributes;
		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
	}

	/**
	 * Since pubsub writes are asynchronous, the user always gets a 200OK
	 * irrespective of what happens downstream. Writing to a bq sink allows
	 * developers to revisit the failed messages, dissect failures and, if they
	 * want, retry messages.
	 * 
	 * ------------------- Table schema ---------------------------- Data: data
	 * passed in user request HttpCode: Failure code returned by PubSub StatusMsg:
	 * Failure message TimeStamp: Timestamp recorded at the time of record entry
	 * -------------------------------------------------------------
	 * 
	 * @param apiException
	 */
	public static void insertFailedMessagesInBQ(Message msg, ApiException apiException, ServletContext ctx) {
		// Read init variables from context and config
		String dataset = ProxyPropertiesUtils.getPropertyValue("dataset");
		String table = ProxyPropertiesUtils.getPropertyValue("table");

		// Get BQ handler from servletcontext
		BigQuery bigquery = (BigQuery) ctx.getAttribute(bigqueryInstance);

		// Values of the row to insert
		Map<String, Object> rowContent = new HashMap<>();
		rowContent.put(data, msg.getData());
		rowContent.put(code, apiException.getStatusCode().getCode().getHttpStatusCode());
		rowContent.put(statusmsg, apiException.getMessage());
		rowContent.put(timestamp, new DateTime(new Date()));

		// Insert values
		TableId tableId = TableId.of(dataset, table);
		bigquery.insertAll(InsertAllRequest.newBuilder(tableId).addRow(rowContent).build());
	}
}
