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

package com.google.pubsub.proxy.util;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.google.pubsub.proxy.actions.publish.PublishMessage;
import com.google.pubsub.proxy.actions.publish.ValidateAccessTokenImpl;
import com.google.pubsub.proxy.exceptions.AccessTokenAuthException;
import com.google.pubsub.proxy.exceptions.GenericAPIException;
import com.google.pubsub.proxy.exceptions.MissingRequiredFieldsException;
import com.google.pubsub.proxy.server.HealthCheck;

/**
 * All custom resources to be injected in 
 * servlet container should be added here
 */
public class InjectResourcesUtils {
	public static ResourceConfig injectResources() {
		ResourceConfig resourceConfig = new ResourceConfig();
		
		// End-point resources
		resourceConfig.register(new PublishMessage());
		resourceConfig.register(new HealthCheck());

		// Providers
		resourceConfig.register(new ValidateAccessTokenImpl());

		// Jackson - json to POJO
		resourceConfig.register(JacksonFeature.class);

		// Custom exception classes written as providers
		resourceConfig.register(new AccessTokenAuthException());
		resourceConfig.register(new MissingRequiredFieldsException());
		resourceConfig.register(new GenericAPIException());

		return resourceConfig;
	}
}
