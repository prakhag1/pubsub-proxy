# Publish messages to Google Cloud Pub/Sub from mobile/client applications

This is not an officially supported Google product.

Copyright 2019 Google LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Introduction
This repository provides implementation of a proxy service which enables client-side apps to publish messages to Google Cloud [Pub/Sub](https://cloud.google.com/pubsub/docs/overview). The proxy service:
- Authenticates incoming end user requests. 
- Forwards authenticated requests to Pub/Sub using appropriate [Cloud IAM](https://cloud.google.com/iam/docs/overview) permissions

The detailed steps to run this proxy on GCP is covered in the tutorial available [here]().

## Google Cloud Products Used or Referenced:
- Cloud PubSub
- Compute Engine
- Cloud Build 
- Cloud Endpoints
- Container Registry

## Request Flow
Any request destined to Pub/Sub goes through Pub/Sub proxy. To identify the calling app that sends requests to the proxy, we will use OpenId Connect Tokens (OIDC). The Pub/Sub proxy makes use of [Cloud Endpoints](https://cloud.google.com/endpoints/docs/openapi/architecture-overview) to authenticate requests from users.

The client sends a request to the Pub/Sub proxy which is intercepted by Cloud Endpoints. Cloud Endpoints validates the OIDC token before forwarding the request to Pub/Sub proxy. Upon receiving the request, Pub/Sub proxy creates an publish request and sends it to Cloud Pub/Sub. For all incoming requests, Cloud Pub/Sub needs to ensure if the caller has the right permissions to issue publish requests. The Pub/Sub proxy uses the [Compute Engine default service account](https://cloud.google.com/compute/docs/access/service-accounts#compute_engine_default_service_account) to authenticate itself with Cloud Pub/Sub.

![Alt text](img/requestflow.png?raw=true)

## Local Deployment & Testing
The local deployment of Pub/Sub proxy (discussed below) has a slight departure from the request flow discussed above. For local testing, Pub/Sub proxy is exposed as an independent entity and not via Cloud Endpoints. This means that the calls do not need to be carry an authentication token to validate the caller with Pub/Sub proxy. 

For production grade deployments, refer to the [detailed solution post]().

Clone repository:
```
git clone https://github.com/GoogleCloudPlatform/solutions-pubsub-proxy-rest
cd solutions-pubsub-proxy-rest
```
Set environment variables:
```
export SERVICE_ACCOUNT_NAME=proxy-test-sa
export SERVICE_ACCOUNT_DEST=sa.json
export TOPIC=test-topic
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/sa.json
export PROJECT=$(gcloud info --format='value(config.project)')
```
Create Pub/Sub topic:
```
gcloud pubsub topics create $TOPIC
```
Create service account:
```
gcloud iam service-accounts create \
   $SERVICE_ACCOUNT_NAME \
   --display-name $SERVICE_ACCOUNT_NAME

SA_EMAIL=$(gcloud iam service-accounts list \
   --filter="displayName:$SERVICE_ACCOUNT_NAME" \
   --format='value(email)')

gcloud projects add-iam-policy-binding $PROJECT \
   --member serviceAccount:$SA_EMAIL \
   --role roles/pubsub.publisher

mkdir -p $(dirname $SERVICE_ACCOUNT_DEST) && \
gcloud iam service-accounts keys create \
   $SERVICE_ACCOUNT_DEST \
   --iam-account $SA_EMAIL
```
### Run Proxy Without Containerizing
To execute test cases and package, run:
```
mvn clean compile assembly:assembly package
```
To skip test cases, run:
```
mvn clean compile assembly:assembly package -DskipTests
```
On a new terminal, start the proxy after changing to the directory where pubsub-proxy was cloned:
```
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/sa.json
java -jar target/pubsub-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar 
```
Back on the original terminal, publish a message to Cloud Pub/Sub via proxy.
```
curl -i -X POST localhost:8080/publish \
   -H "Content-Type: application/json" \
   -d '{"topic": "'$TOPIC'", "messages": [ {"attributes": {"key1": "value1", "key2" : "value2"}, "data": "test data"}]}'
```
On the terminal running the proxy, check the logs to verify if the message was successfully published to Pub/Sub.

### Containerize Proxy
Once the standalone proxy works, proceed to containerize the application for local testing.
Build docker image:
```
docker build -t pubsub-proxy .
```
Check if the image was successfully created:
```
docker images | grep pubsub-proxy
```
Build a container using the docker image:
```
docker run -v $(pwd)/sa.json:/tmp/sa.json \
   -d --rm --name pubsub-proxy \
   -e "GOOGLE_APPLICATION_CREDENTIALS=/tmp/sa.json" pubsub-proxy
```
Check if the container was successfully created:
```
docker ps | grep pubsub-proxy
```
Test proxy:
```
docker exec -it pubsub-proxy \
   curl -i -X POST localhost:8080/publish \
   -H "Content-Type: application/json" \
   -d '{"topic": "'$TOPIC'", "messages": [ {"attributes": {"key1": "value1", "key2" : "value2"}, "data": "test data"}]}'
```
Check logs:
```
docker logs pubsub-proxy
```
### Deploy Proxy on GKE
Detailed steps to run this proxy on GCP is covered in the tutorial [here]().

## Cleaning Up
Remove the private key:
```
rm -rf $SERVICE_ACCOUNT_DEST
```
Stop the running docker container:
```
docker stop pubsub-proxy
```
