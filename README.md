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
- Container Registry

## Request Flow
Any request destined to Pub/Sub goes through the proxy. To identify the calling app that sends requests to the proxy, we will use a JSON Web Token (JWT). The application backend generates a JWT and uses a [GCP service account's](https://cloud.google.com/iam/docs/understanding-service-accounts) private key to sign the token. The signed JWT is used to authenticate calls from the client-app to the proxy. 

The proxy forwards the validated requests to Cloud Pub/Sub. Pub/Sub uses Cloud IAM to verify whether the proxy has the right permissions to publish messages. The service account that was used to sign and verify the JWT, will also be used to authenticate calls from the proxy to the Cloud Pub/Sub API.

![Alt text](img/requestflow.png?raw=true)

## Local Deployment & Testing
Clone repository:
```
git clone https://github.com/GoogleCloudPlatform/pubsub-proxy
```
Create service account:
This Service Account would be used to sign and verify the access token as well as setup authentication between the proxy and Cloud Pub/Sub. The service account is passed as an environmet variable (GOOGLE_APPLICATION_CREDENTIALS). In the actual deployment on GKE, the service account credentials are passed as a [Kubernetes Secret](https://kubernetes.io/docs/concepts/configuration/secret/) to GOOGLE_APPLICATION_CREDENTIALS.
```
echo "export SERVICE_ACCOUNT_NAME=proxy-test-sa" >> ~/.bashrc
echo "export SERVICE_ACCOUNT_DEST=sa.json" >> ~/.bashrc
echo "export TOPIC=test-topic" >> ~/.bashrc

gcloud iam service-accounts create \
   $SERVICE_ACCOUNT_NAME \
   --display-name $SERVICE_ACCOUNT_NAME

SA_EMAIL=$(gcloud iam service-accounts list \
   --filter="displayName:$SERVICE_ACCOUNT_NAME" \
   --format='value(email)')

gcloud projects add-iam-policy-binding [PROJECT_NAME] \
   --member serviceAccount:$SA_EMAIL \
   --role roles/pubsub.publisher

gcloud projects add-iam-policy-binding [PROJECT_NAME] \
   --member serviceAccount:$SA_EMAIL \
   --role roles/iam.serviceAccountActor

mkdir -p $(dirname $SERVICE_ACCOUNT_DEST) && \
gcloud iam service-accounts keys create \
   $SERVICE_ACCOUNT_DEST \
   --iam-account $SA_EMAIL
```
Create Pub/Sub topic:
```
gcloud pubsub topics create $TOPIC
```
### Run Proxy Without Containerizing
Export environment variable to include the service account details:
```
echo "export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/$SERVICE_ACCOUNT_DEST" >> ~/.bashrc
```
To execute test cases and package, run:
```
mvn clean compile assembly:assembly package
```
To skip test cases, run:
```
mvn clean compile assembly:assembly package -DskipTests
```
On a new terminal, start the proxy after changing to the appropriate directory:
```
source ~/.bashrc
java -jar target/pubsub-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar 
```
Back on the original terminal, generate a JWT signed by the service account. 
```
npm install --global jsonwebtokencli --loglevel=error

cat $SERVICE_ACCOUNT_DEST | jq -r '.private_key' > private.key

TOKEN=$(jwt --encode --algorithm 'RS256' \
            --private-key-file './private.key' \
            '{"alg":"RS256", "sub":"'$SA_EMAIL'", "iss":"'$SA_EMAIL'", "name":"John Doe", "admin":true}')
```
Publish a message to Cloud Pub/Sub:
```
curl -i POST localhost:8080/publish \
   -H "Authorization: Bearer $TOKEN" \
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
   -d --rm --name proxy \
   -e "GOOGLE_APPLICATION_CREDENTIALS=/tmp/sa.json" pubsub-proxy
```
Check if the container was successfully created:
```
docker ps | grep proxy
```
Test proxy:
```
docker exec -it $(docker ps | grep runtime | awk -F" " '{print $1}') \
   curl -v POST localhost:8080/publish \
   -H "Authorization: Bearer $TOKEN" \
   -H "Content-Type: application/json" \
   -d '{"topic": "'$TOPIC'", "messages": [ {"attributes": {"key1": "value1", "key2" : "value2"}, "data": "test data"}]}'
```
Check logs:
```
docker logs proxy
```
###### Deploy Proxy on GKE
Detailed steps to run this proxy on GCP is covered in the tutorial [here]().

## Cleaning Up
Remove the private keys:
```
rm -rf $SERVICE_ACCOUNT_DEST private.key 
```
Stop the running docker container:
```
docker stop proxy
```
## Additional Functionalities
To add functionalities such as traffic filtering and rate limiting to the proxy, we can use the [Istio](https://istio.io) add-on. There is going to be no change to the application code but the proxy needs to be re-deployed with Istio. Detailed walk through of the steps involved are convered in the tutorial [here](). 

