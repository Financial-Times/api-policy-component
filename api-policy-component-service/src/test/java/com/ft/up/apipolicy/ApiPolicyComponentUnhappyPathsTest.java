package com.ft.up.apipolicy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.io.File;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;


/**
 * ApiPolicyComponentTest
 *
 * @author sarah.wells
 */
public class ApiPolicyComponentUnhappyPathsTest {

    public static final String EXAMPLE_PATH = "/example";
    private static final int SOME_PORT = (int)(Math.random() * 10000) + 40000;

    private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";
    private static final String ERROR_JSON = "{\"message\":\"Something went wrong\"}";
    private static final String SERVER_ERROR_JSON = "{\"message\":\"server error\"}";
    private static final String CONNECT_TIMEOUT_JSON = "{\"message\":\"org.apache.http.conn.ConnectTimeoutException: Connect to localhost:" + (SOME_PORT + 2) + " timed out\"}";
    private static final String SOCKET_TIMEOUT_JSON = "{\"message\":\"java.net.SocketTimeoutException: Read timed out\"}";

    @Rule
    public WireMockRule wireMockForVarnish1 = new WireMockRule(SOME_PORT);
    @Rule
    public WireMockRule wireMockForVarnish2 = new WireMockRule(SOME_PORT + 2);

    @Rule
    public DropwizardAppRule<ApiPolicyConfiguration> policyComponent = new DropwizardAppRule<>(
            ApiPolicyApplication.class,
            resourceFilePath("config-junit.yml"),
            config("varnish.primaryNodes",
                    String.format("localhost:%d:%d, localhost:%d:%d", 
                            SOME_PORT, SOME_PORT +1, 
                            SOME_PORT + 2, SOME_PORT + 3 )
            )
    );

    private Client client;
    
    private int leasedConnections = 0;

    @Before
    public void setup() {

        this.client = Client.create();
        leasedConnections = getLeasedConnections("semanticReaderVarnish");
    }

    @After
    public void checkThatNumberOfLeasedConnectionsHaveNotChanged(){
        assertThat("connections should not leak", leasedConnections, equalTo(getLeasedConnections("semanticReaderVarnish")));
        WireMock.reset();
    }

    // Fails completely    
    @Test
    public void shouldFailWhereBothNodesAreReturning500() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(500)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(SERVER_ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(500)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(500));//TODO should map downstream errors to a 503
            assertThat(response.getEntity(String.class), is(SERVER_ERROR_JSON));

        } finally {
            response.close();
        }
    }
    
    @Test
    public void shouldFailWhereBothNodesAreReturning503() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(503)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(SERVER_ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(503)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(503));
            assertThat(response.getEntity(String.class), is(SERVER_ERROR_JSON));

        } finally {
            response.close();
        }
    }
    

    
    @Test
    public void shouldReturnErrorIfTimeoutOccurs() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withFixedDelay(2000)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withFixedDelay(2000)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(503));
            assertThat(response.getEntity(String.class), anyOf(is(CONNECT_TIMEOUT_JSON), is(SOCKET_TIMEOUT_JSON)));

        } finally {
            response.close();
        }
    }
    
    // First request fails, second request is OK
    @Test
    public void shouldReturnSuccessFromSecondNodeWhereRecoverableErrorOccursForFirstNode() {
        
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH))
                .willReturn(
                        aResponse()
                        .withBody(ERROR_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(500)
                ));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH))
                .willReturn(
                        aResponse()
                        .withBody(EXAMPLE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
                ));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();
        
        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
    }

    private UriBuilder fromFacade(String path) {
        return UriBuilder.fromPath(path).host("localhost").port(policyComponent.getLocalPort()).scheme("http");
    }


    public static String resourceFilePath(String resourceClassPathLocation) {

        File file = null;

        try {

            file = new File(Resources.getResource(resourceClassPathLocation).toURI());
            return file.getAbsolutePath();

        } catch (Exception e) {
            if(file!=null) {
                throw new RuntimeException(file.toString(), e);
            }
            throw new RuntimeException(e);
        }
    }


    private int getLeasedConnections(String name){
        return client.resource("http://localhost:" + 21082).path("/metrics") //hardcoded because we have no access to getAdminPort() on the app rule
                .get(JsonNode.class)
                .get("gauges")
                .get("org.apache.http.conn.ClientConnectionManager." + name + ".leased-connections")
                        .get("value").asInt();

    }
}
