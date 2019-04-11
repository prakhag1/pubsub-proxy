package com.google.pubsub.proxy.actions.publish;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.pubsub.proxy.exceptions.AccessTokenAuthException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ValidateAccessTokenImplTest {

    protected static class AccessToken {

        public String fetchToken(ServletContext context) {
            ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) context.getAttribute("serviceaccount");
            return fetchToken(serviceAccount.getClientEmail(), serviceAccount.getPrivateKey());
        }

        public String fetchToken(String email, PrivateKey privateKey) {
            JwtBuilder jwts = Jwts.builder();

            // Set header
            Map<String, Object> map = new HashMap<>();
            map.put("type", "JWT");
            map.put("alg", "RS256");
            jwts.setHeader(map);

            // Set claims
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("sub", email);
            claims.put("exp", System.currentTimeMillis() / 1000 + 3600);
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("iss", email);
            jwts.setClaims(claims);

            // Sign with key
            jwts.signWith(SignatureAlgorithm.RS256, privateKey);
            return jwts.compact();

        }

    }

    private static final String INVALID_TOKEN = "INVALID_JWT_TOKEN";
    private static final String EXPIRED_TOKEN = "eyJ0eXBlIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJuZXR3b3JrLWFkbWluQHF3aWtsYWJzLWdjcC0wYjA3NzYzYmQ5YjM2ZDkzLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiaXNzIjoibmV0d29yay1hZG1pbkBxd2lrbGFicy1nY3AtMGIwNzc2M2JkOWIzNmQ5My5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsImV4cCI6MTU1MjQ3NzY2MiwiaWF0IjoxNTUyNDc0MDYyfQ.B9uFnwHTE8DArQr1pgKYM5L4Lu_LiP4zPeyhADD8GH5h9m8faDoCc_gkMz-RVmKtbTE4xQLoeoBjJKhTdiUUKmpPy1LdIzF6L_1WQxWibAaNd2skR6GAQrDw1TUDPAogYU-8aU0nQAsiooy-8dPk8LoFHZqq47r06HWQOv_4lPvLidM2sJPan67pJ3k_ArFelfJuSuNVK1tHPK-fBY32zfuMkHWNC_5NJQtLigF04iSO5OrdMsePQgATtFO9b3Xjs7z2yoOoldAbQSuJ4hEwVa-GBCoHZLOyZU-LyrS9GUnwn6h_6GGCZyPBTA0otUO3Lw4OnRVjUS1jLVKloabMNA";
    private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";
    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";
    private static final String TEST_CRED_FILE = System.getProperty("SERVICE_ACCOUNT_CREDENTIALS", "src/test/resources/credentials.json");

    @Mock
    ContainerRequestContext context;

    @Mock
    ServletContext servletContext;

    @Spy
    ValidateAccessTokenImpl validateAccessTokenImpl;

    @Spy
    AccessToken accessToken;

    @Before
    public void setUp() throws Exception {
        InputStream credsStream = new FileInputStream(TEST_CRED_FILE);
        ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(credsStream);
        when(servletContext.getAttribute("serviceaccount")).thenReturn(serviceAccount);
        validateAccessTokenImpl.ctx = servletContext;
    }

    @After
    public void tearDown() {
    }

    @Test(expected = AccessTokenAuthException.class)
    public void WhenAccessTokenIsNotPresentThenAccessTokenExceptionIsThrown() throws IOException {
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        validateAccessTokenImpl.filter(context);
    }

    @Test(expected = AccessTokenAuthException.class)
    public void WhenAccessTokenIsPresentButInvalidThenAccessTokenExceptionIsThrown() throws IOException {
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(INVALID_TOKEN);
        validateAccessTokenImpl.filter(context);
    }

    @Test(expected = AccessTokenAuthException.class)
    public void WhenAccessTokenIsPresentButIsNotABearerTokenThenAccessTokenExceptionIsThrown() throws IOException {
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHENTICATION_SCHEME_BASIC + " " + INVALID_TOKEN);
        validateAccessTokenImpl.filter(context);
    }

    @Test(expected = AccessTokenAuthException.class)
    public void WhenAccessTokenIsPresentAndIsValidButExpiredThenAccessTokenExceptionIsThrown() throws IOException {
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHENTICATION_SCHEME_BEARER + " " + EXPIRED_TOKEN);
        validateAccessTokenImpl.filter(context);
    }

    @Test
    public void WhenAccessTokenIsPresentAndIsValidThenNoErrorIsThrown() throws Exception {
        String validToken = accessToken.fetchToken(servletContext);
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHENTICATION_SCHEME_BEARER + " " + validToken);
        validateAccessTokenImpl.filter(context);
    }
}