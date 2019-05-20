package com.google.pubsub.proxy.actions.publish;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.pubsub.proxy.entities.Message;
import com.google.pubsub.proxy.entities.Request;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

public class PublishMessageTest extends JerseyTest {

    private static final String TEST_CRED_FILE = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS", "src/test/resources/credentials.json");
    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";
    private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";
    private static final String TOPIC = "TOPIC";
    private static final String BAD_ACCESS_TOKEN = "INVALID_CLIENT_ID";
    private Request request;
    private Entity<Request> requestEntity;
    private String GOOD_ACCESS_TOKEN = null;
    private List<Message> messages = null;

    @Override
    protected DeploymentContext configureDeployment() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);
        return ServletDeploymentContext
                .forServlet(PublishMessageTest.TestServlet.class)
                .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Before
    public void setUpTest() throws Exception {
        request = new Request();
        request.setTopic(TOPIC);
        messages = new ArrayList<>();
        request.setMessages(messages);
        requestEntity = Entity.entity(request, MediaType.APPLICATION_JSON);
        GOOD_ACCESS_TOKEN = getAccessToken();
    }

    private String getAccessToken() throws Exception {
        InputStream credsStream = new FileInputStream(TEST_CRED_FILE);
        ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(credsStream);
        return new AccessToken().fetchToken(serviceAccount.getClientEmail(), serviceAccount.getPrivateKey());
    }

    @After
    public void tearDownTest(){

    }

    @Test
    public void WhenAccessTokenIsNotPresentErrorIsReturned() {
        Response response = target("/publish").request().post(requestEntity);
        validate(response, Response.Status.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void WhenAccessTokenIsPresentButInvalidErrorIsReturned() {
        Response response = target("/publish")
                .request()
                .header(HttpHeaders.AUTHORIZATION, BAD_ACCESS_TOKEN)
                .post(requestEntity);
        validate(response, Response.Status.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void WhenAccessTokenIsPresentButInvalidAuthSchemeErrorIsReturned() throws Exception {
        Response response = target("/publish")
                .request()
                .header(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME_BASIC + " " + GOOD_ACCESS_TOKEN)
                .post(requestEntity);
        validate(response, Response.Status.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void WhenAccessTokenIsPresentAndValidButNoMessagesThenErrorIsReturned() throws Exception {
        Response response = target("/publish")
                .request()
                .header(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME_BEARER + " " + GOOD_ACCESS_TOKEN)
                .post(requestEntity);
        validate(response, Response.Status.INTERNAL_SERVER_ERROR);
    }

    private void validate(Response response, Response.Status status) {
        Assert.assertEquals(status.getStatusCode(), response.getStatus());
    }

    protected static class AccessToken {
        public String fetchToken(String email, PrivateKey privateKey) {
            return new ValidateAccessTokenImplTest.AccessToken().fetchToken(email, privateKey);
        }
    }

    protected static class TestValidator extends  ValidateAccessTokenImpl {

    }

    protected static class TestServlet extends ServletContainer {

        public TestServlet(){
            super(new ResourceConfig(PublishMessage.class).register(PublishMessageTest.TestValidator.class));
        }

        @Override
        public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            InputStream credsStream = new FileInputStream(TEST_CRED_FILE);
            ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(credsStream);
            this.getServletContext().setAttribute("serviceaccount", serviceAccount);
            super.service(req, resp);
        }
    }


}