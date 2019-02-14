package com.google.pubsub.proxy.util;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.google.pubsub.proxy.actions.accesstoken.GetAccessToken;
import com.google.pubsub.proxy.actions.accesstoken.ValidateEndClientImpl;
import com.google.pubsub.proxy.actions.publish.PublishMessage;
import com.google.pubsub.proxy.actions.publish.ValidateAccessTokenImpl;
import com.google.pubsub.proxy.exceptions.AccessTokenAuthException;
import com.google.pubsub.proxy.exceptions.ClientMismatchException;
import com.google.pubsub.proxy.exceptions.GenericAPIException;
import com.google.pubsub.proxy.exceptions.MissingRequiredFieldsException;
import com.google.pubsub.proxy.server.HealthCheck;

/**
 * All custom resources to be injected in servlet container find a place here 
 */
public class InjectResourcesUtils {
		
	public static ResourceConfig injectResources() {
		
		ResourceConfig resourceConfig = new ResourceConfig();
		
		//End-point resources
		resourceConfig.register(new PublishMessage());
		resourceConfig.register(new GetAccessToken());
		resourceConfig.register(new HealthCheck());
		
		//Providers
		resourceConfig.register(new ValidateAccessTokenImpl());
		resourceConfig.register(new ValidateEndClientImpl());
		
		//Jackson - json to POJO
		resourceConfig.register(JacksonFeature.class);
		
		//Custom exception classes written as providers
		resourceConfig.register(new ClientMismatchException());
		resourceConfig.register(new AccessTokenAuthException());
		resourceConfig.register(new MissingRequiredFieldsException());
		resourceConfig.register(new GenericAPIException());
		
		return resourceConfig;
	}
}
