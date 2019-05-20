package com.google.pubsub.proxy.server;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.google.pubsub.proxy.server.WebServer.main;


public class WebServerTest {

    protected static class ThreadRunner implements Runnable {

        @Override
        public void run() {
            try {
                main(new String[]{});
            }
            catch (Exception e) {
                System.out.println("Unable to start the application");
            }
        }
    }


    @Before
    public void setUp() {
        new Thread(new ThreadRunner()).start();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void whenTheApplicationStartsJettyServerIsStarted() throws IOException, InterruptedException {
        Thread.sleep(45000);
        HttpUriRequest request = new HttpGet("http://localhost:8080");
        HttpResponse response = HttpClientBuilder.create().build().execute(request);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }
}