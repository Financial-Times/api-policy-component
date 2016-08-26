package com.ft.up.apipolicy;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.IOUtils;
import org.fest.util.Strings;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dropwizard.testing.junit.DropwizardAppRule;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.ft.up.apipolicy.JsonConverter.JSON_MAP_TYPE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * ApiPolicyComponentTest
 *
 * @author Simon.Gibbs
 */
public class ApiPolicyComponentHappyPathsTest {

    private static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";

    public static final String EXAMPLE_PATH = "/example";
    public static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";
    public static final String CONTENT_PATH_WITH_WEB_URL = "/content/34b43ca3-fbea-4677-add7-c0aa47101d5b";
    public static final String CONTENT_PATH_2 = "/content/f3b60ad0-acda-11e2-a7c4-002128161462";
    public static final String ENRICHED_CONTENT_PATH = "/enrichedcontent/bcafca32-5bc7-343f-851f-fd6d3514e694";
    public static final String BASE_NOTIFICATION_PATH = "/content/notifications?since=2014-10-15&type=article";
    public static final String FOR_BRAND = "&forBrand=";
    public static final String NOT_FOR_BRAND = "&notForBrand=";
    public static final String PLAIN_NOTIFICATIONS_FEED_URI = "http://contentapi2.ft.com/content/notifications?since=2014-10-15";
    public static final String SUGGEST_PATH = "/suggest";
    public static final String QUERY_PARAM_NAME = "curatedTopStoriesFor";
    public static final String QUERY_PARAM_VALUE = "f9c5eaed-d7e1-47f1-b6a0-470c9e26ab0e";
    public static final String RICH_CONTENT_KEY = "INCLUDE_RICH_CONTENT";
    public static final String EXAMPLE_TRANSACTION_ID = "010101";
    private static final int SOME_PORT = (int) (Math.random() * 10000) + 40000;
    private static final String LIST_UUID = "9125b25e-8305-11e5-8317-6f9588949b85";
    private static final String LISTS_BASE_PATH = "/lists";
    public static final String LISTS_PATH = LISTS_BASE_PATH + "/" + LIST_UUID;
    private static final String PARAM_VALIDATE_LINKS = "validateLinkedResources";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiPolicyComponentHappyPathsTest.class);

    private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";
    private static final String CONTENT_JSON =
            "{" +
                "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\",\n" +
                "\"openingXML\" : \"<body>a video</body>\",\n" +
                "\"alternativeTitles\" : {},\n" +
                "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                "\"identifiers\": [{\n" +
                "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                "\"identifierValue\": \"220322\"\n" +
                "}]" +
            "}";
    private static final String CONTENT_PATH_WITH_WEB_URL_JSON =
            "{" +
                "\"uuid\": \"34b43ca3-fbea-4677-add7-c0aa47101d5b\", " +
                "\"bodyXML\" : \"<body>some article</body>\",\n" +
                "\"openingXML\" : \"<body>some article</body>\",\n" +
                "\"alternativeTitles\" : {},\n" +
                "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                "\"identifiers\": [{\n" +
                "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                "\"identifierValue\": \"220322\"\n" + "}],\n" +
                "\"webUrl\": \"http://www.ft.com/some-vanity-url\"\n" +
                "}";

    private static final String ENRICHED_CONTENT_JSON =
            "{" +
                "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\", " +
                "\"openingXML\" : \"<body>a video</body>\",\n" +
                "\"alternativeTitles\" : {},\n" +
                "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                "\"identifiers\": [{\n" +
                "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                "\"identifierValue\": \"220322\"\n" +
                "}]," +
                "\"brands\": [ ],\n" +
                "\"annotations\": [ ]" +
            "}";
	private static final String RICH_CONTENT_JSON = "{" +
				"\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
				"\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\", " +
                "\"identifiers\": [{\n" +
                "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                "\"identifierValue\": \"220322\"\n" +
                "}]" +
			"}";
	private static final String SUGGEST_REQUEST_JSON =
	        "{"
	        + "\"body\": \"Test content\""
	        + "}";
    private static final String SUGGEST_RESPONSE_JSON =
            "{"
            + "\"suggestions\": [ ]"
            + "}";
    private static final String LISTS_JSON =
            "{" +
                "\"id\": \"http://api.ft.com/things/9125b25e-8305-11e5-8317-6f9588949b85\", " +
                "\"title\": \"Home-INTL Top Stories\", " +
                "\"apiUrl\": \"http://int.api.ft.com/lists/9125b25e-8305-11e5-8317-6f9588949b85\", " +
                "\"layoutHint\": \"Standard\", " +
                "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                "\"publishReference\": \"tid_vcxz08642\" " +
            "}";
    private static final String NOTIFICATIONS_RESPONSE_TEMPLATE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15%s\", " +
            "\"notifications\": [ %s ], " +
            "\"links\": [] " +
            "}";
    private static final String NOTIFICATIONS = "{ \"type\": \"http://www.ft.com/thing/ThingChangeType/UPDATE\", " +
            "\"id\": \"http://www.ft.com/thing/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"apiUrl\": \"http://int.api.ft.com/content/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"publishReference\": \"tid_AbCd1203\", " +
            "\"lastModified\": \"2015-12-13T17:04:54.636Z\"" +
            " } ";
    private static final String ALL_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "", NOTIFICATIONS);
    private static final String FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, FOR_BRAND + FASTFT_BRAND, "");
    private static final String NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, NOT_FOR_BRAND + FASTFT_BRAND, "");
    private static final String FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE,
            FOR_BRAND + FASTFT_BRAND + NOT_FOR_BRAND + FASTFT_BRAND, "");
    @Rule
    public WireMockRule wireMockForVarnish = new WireMockRule(SOME_PORT);
    @Rule
    public DropwizardAppRule<ApiPolicyConfiguration> policyComponent = new DropwizardAppRule<>(
            ApiPolicyApplication.class,
            resourceFilePath("config-junit.yml"),
            config("varnish.primaryNodes",
                    String.format("localhost:%d:%d, localhost:%d:%d",
                            SOME_PORT, SOME_PORT + 1,
                            SOME_PORT + 2, SOME_PORT + 3)
            )
    );
    int leasedConnectionsBeforeForContent = 0;
    int leasedConnectionsBeforeForNotifications = 0;
    int leasedConnectionsBeforeForEnrichedContent = 0;
    int leasedConnectionsBeforeForOther = 0;
    private Client client;
    private ObjectMapper objectMapper;

    public static String resourceFilePath(String resourceClassPathLocation) {

        File file = null;

        try {

            file = new File(Resources.getResource(resourceClassPathLocation).toURI());
            return file.getAbsolutePath();

        } catch (Exception e) {
            if (file != null) {
                throw new RuntimeException(file.toString(), e);
            }
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() {
        stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(WireMock.post(urlEqualTo(SUGGEST_PATH)).willReturn(aResponse().withBody(SUGGEST_RESPONSE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(WireMock.get(urlPathEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(WireMock.get(urlPathEqualTo(CONTENT_PATH_WITH_WEB_URL)).willReturn(aResponse().withBody(CONTENT_PATH_WITH_WEB_URL_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        this.client = Client.create();
        objectMapper = new ObjectMapper();
        leasedConnectionsBeforeForContent = getLeasedConnections("content");
        leasedConnectionsBeforeForNotifications = getLeasedConnections("notifications");
        leasedConnectionsBeforeForEnrichedContent = getLeasedConnections("enrichedcontent");
        leasedConnectionsBeforeForOther = getLeasedConnections("other");

    }

    @After
    public void checkThatNumberOfLeasedConnectionsHaveNotChanged(){
        assertThat(leasedConnectionsBeforeForContent, equalTo(getLeasedConnections("content")));
        assertThat(leasedConnectionsBeforeForNotifications, equalTo(getLeasedConnections("notifications")));
        assertThat(leasedConnectionsBeforeForEnrichedContent, equalTo(getLeasedConnections("enrichedcontent")));
        assertThat(leasedConnectionsBeforeForOther, equalTo(getLeasedConnections("other")));
        WireMock.reset();
    }

    @Test
    public void shouldAllowUnknownRequestsThrough() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldAllowSuggestPostRequestsThrough() {
        URI uri  = fromFacade(SUGGEST_PATH).build();

        ClientResponse response = client.resource(uri)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, SUGGEST_REQUEST_JSON);

        try {
            verify(postRequestedFor(urlEqualTo(SUGGEST_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), is(SUGGEST_RESPONSE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardUnknownHeaders() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).header("Arbitrary", "Example").get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader("Arbitrary", equalTo("Example")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardTransactionId() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).header(TransactionIdUtils.TRANSACTION_ID_HEADER, EXAMPLE_TRANSACTION_ID).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader(TransactionIdUtils.TRANSACTION_ID_HEADER, equalTo("010101")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGenerateAndForwardTransactionIdIfMissing() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader(TransactionIdUtils.TRANSACTION_ID_HEADER, containing("tid_")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetTheContentWithExtraWebUrlField() throws IOException {
        URI uri  = fromFacade(CONTENT_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH)));

            Map<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "http://www.ft.com/fastft/220322");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetTheContentWithExistingWebUrlField() throws IOException {
        URI uri  = fromFacade(CONTENT_PATH_WITH_WEB_URL).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_WITH_WEB_URL)));

            Map<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "http://www.ft.com/some-vanity-url");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetEnrichedContentWithExtraWebUrlField() throws IOException {

        stubFor(WireMock.get(urlPathEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        URI uri  = fromFacade(ENRICHED_CONTENT_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlPathEqualTo(ENRICHED_CONTENT_PATH)));

            Map<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "http://www.ft.com/fastft/220322");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldTreatMultiplePolicyHeadersTheSame() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + URLEncoder.encode(FASTFT_BRAND, "UTF-8")
                + NOT_FOR_BRAND + URLEncoder.encode(FASTFT_BRAND, "UTF-8");

        stubForNotifications(url, FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON);

        /*

        Drop to the TCP layer to simulate a strangely formatted HTTP request
        Making sure this works is important for simplifying ApiGee and since socket programming
        is really easy what's the harm?

        ;-)

        */

        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            socket = new Socket(facadeUri.getHost(), facadeUri.getPort());

            writer = new PrintWriter( socket.getOutputStream() );
            reader = new BufferedReader( new InputStreamReader( socket.getInputStream() )); // the buffer enables readLine()


            writer.println("GET /content/notifications?since=2014-10-15 HTTP/1.1");
            writer.println("Host: " + facadeUri.getAuthority()); // I think we want the port number so "authority" not "host"
            writer.println("X-Policy: FASTFT_CONTENT_ONLY");
            writer.println("X-Policy: EXCLUDE_FASTFT_CONTENT");
            writer.println();

            // SEND the request
            writer.flush();

            String line = reader.readLine();

            // stop at the blank line, so we don't wait on the buffer refilling.
            while(!Strings.isNullOrEmpty(line)) {
                LOGGER.info(line);
                line = reader.readLine();
            }

        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(socket);
        }

        // after all that, we're only really interested in whether the app called the varnish layer with the same parameters.
        verify(getRequestedFor(urlEqualTo(url)));

    }

    @Test
    public void givenNoFastFtRelatedPolicyShouldGetNotificationsWithNoBrandParameter() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH;

        stubForNotifications(url, ALL_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl, is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenPolicyFASTFT_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + URLEncoder.encode(FASTFT_BRAND, "UTF-8");

        stubForNotifications(url, FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }
    }

    @Test
    public void givenPolicyEXCLUDE_FASTFT_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND + URLEncoder.encode(FASTFT_BRAND, "UTF-8");

        stubForNotifications(url, NOT_FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenListedPoliciesFASTFT_CONTENT_ONLYCommaEXCLUDE_FASTFT_CONTENTShouldProcessBothAsNormal() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + URLEncoder.encode(FASTFT_BRAND, "UTF-8")
                + NOT_FOR_BRAND + URLEncoder.encode(FASTFT_BRAND, "UTF-8");

        stubForNotifications(url, FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY, EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);


        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenVaryHeaderWithAcceptShouldAddXPolicy() {
        URI uri  = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withHeader("Vary", "Accept")));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getHeaders().get("Vary").size(), is(1));

            List<String> varyHeaderValue = atomise(response.getHeaders().get("Vary"));
            assertThat(varyHeaderValue, hasItems("Accept", HttpPipeline.POLICY_HEADER_NAME));

        } finally {
            response.close();
        }
    }

    private URI sinceSomeDateFromFacade() {
        return fromFacade("/content/notifications")
                .queryParam("since","2014-10-15")
                .build();
    }

    private void stubForNotifications(String url, String responseBody) {
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private List<String> atomise(List<String> varyHeaderValues) {
        List<String> result = Lists.newArrayList();
        for(String varyHeaderValue : varyHeaderValues) {
            result.addAll(Arrays.asList(varyHeaderValue.split("[ ,]")));
        }

        return result;
    }

    @Test
    public void shouldAddVaryHeaderWithXPolicy() {
        URI uri  = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            List<String> varyHeaderValue = response.getHeaders().get("Vary");

            assertThat(varyHeaderValue.size(), is(1));
            assertThat(varyHeaderValue, hasItems(HttpPipeline.POLICY_HEADER_NAME));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldPassDownArbitraryResponseHeadersUnlessBlackListed() {

        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(
                aResponse()
                        .withBody(CONTENT_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
                        .withHeader("X-Example", "100")
                        .withHeader("Accept-Encoding", "test") // out of place for a response, but this is a test
        ));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            assertThat(response.getHeaders().getFirst("X-Example"), is("100"));
            assertThat(response.getHeaders().getFirst("Accept-Encoding"),nullValue());
        } finally {
            response.close();
        }
    }

	@Test
	public void givenRICH_CONTENTIsOnIShouldReceiveRichContent() {
		URI uri = fromFacade(CONTENT_PATH_2).build();

		stubForRichContentWithYouTubeVideo();

		ClientResponse response = client.resource(uri)
			.header(HttpPipeline.POLICY_HEADER_NAME, RICH_CONTENT_KEY)
			.get(ClientResponse.class);

		try {
			verify(
			    getRequestedFor(urlPathEqualTo(CONTENT_PATH_2))
			      .withQueryParam(PARAM_VALIDATE_LINKS, equalTo("true")));

			assertThat(response.getStatus(), is(200));

			String json = response.getEntity(String.class);

			assertThat(json,containsString("youtube.com"));


		} finally {
			response.close();
		}
	}

	@Test
	public void givenRICH_CONTENTIsOffIShouldNotReceiveRichContent() {
		URI uri = fromFacade(CONTENT_PATH_2).build();

		stubForRichContentWithYouTubeVideo();

		ClientResponse response = client.resource(uri).get(ClientResponse.class);

		try {
			verify(getRequestedFor(urlEqualTo(CONTENT_PATH_2)));

			assertThat(response.getStatus(), is(200));

			String json = response.getEntity(String.class);

			assertThat(json,not(containsString("youtube.com")));


		} finally {
			response.close();
		}
	}

    @Test
    public void shouldLeaveLastModifiedInJsonWhenPolicyIncludeIsPresentForLists() throws Exception {
        final URI uri = fromFacade(LISTS_PATH).build();
        stubFor(WireMock.get(urlEqualTo(LISTS_PATH)).willReturn(aResponse().withBody(LISTS_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INCLUDE_LAST_MODIFIED_DATE.getHeaderValue())
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(LISTS_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), containsJsonProperty("lastModified"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardListsCallWithQueryParameters() throws Exception {

        final URI uri = fromFacade(LISTS_BASE_PATH, ImmutableMap.of(QUERY_PARAM_NAME, (Object) QUERY_PARAM_VALUE)).build();
        stubFor(WireMock.get(urlPathEqualTo(LISTS_BASE_PATH))
                .withQueryParam(QUERY_PARAM_NAME, equalTo(QUERY_PARAM_VALUE))
                .willReturn(aResponse().withBody(LISTS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlPathMatching(LISTS_BASE_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), not(containsJsonProperty("lastModified")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldRemoveLastModifiedFromJsonForLists() throws Exception {
        final URI uri = fromFacade(LISTS_PATH).build();
        stubFor(WireMock.get(urlEqualTo(LISTS_PATH)).willReturn(aResponse().withBody(LISTS_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(LISTS_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), not(containsJsonProperty("lastModified")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldAddWebUrlForContent() {
        final URI uri = fromFacade(CONTENT_PATH).build();
        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.getEntity(String.class);
            assertThat(jsonPayload, containsJsonProperty("webUrl"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldAddWebUrlForEnrichedContent() {
        final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
        stubFor(WireMock.get(urlEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(ENRICHED_CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.getEntity(String.class);
            assertThat(jsonPayload, containsJsonProperty("webUrl"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldRemovePublishReferenceAndLeaveAllOthersInJSONForLists() {
        final URI uri = fromFacade(LISTS_PATH).build();
        stubFor(WireMock.get(urlEqualTo(LISTS_PATH)).willReturn(aResponse().withBody(LISTS_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(LISTS_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.getEntity(String.class);
            assertThat(jsonPayload, not(containsJsonProperty("publishReference")));
            assertThat(jsonPayload, containsJsonProperty("id"));
            assertThat(jsonPayload, containsJsonProperty("title"));
            assertThat(jsonPayload, containsJsonProperty("apiUrl"));
            assertThat(jsonPayload, containsJsonProperty("layoutHint"));
        } finally {
            response.close();
        }

    }

    @Test
    public void shouldRemovePublishReferenceAndLastModifiedAndLeaveAllOthersInJSONForNotifications() {
        final URI uri = fromFacade("/content/notifications")
                .queryParam("since", "2014-10-15")
                .build();
        stubFor(WireMock.get(urlEqualTo(BASE_NOTIFICATION_PATH))
                .willReturn(aResponse()
                        .withBody(ALL_NOTIFICATIONS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200))
        );
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(BASE_NOTIFICATION_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.getEntity(String.class);
            assertThat(jsonPayload, not(containsNestedJsonProperty("notifications", "publishReference")));
            assertThat(jsonPayload, not(containsNestedJsonProperty("notifications", "lastModified")));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "type"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "id"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "apiUrl"));
            assertThat(jsonPayload, containsJsonProperty("requestUrl"));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldLeaveLastModifiedAndLeaveAllOthersInJSONForNotifications() {
        final URI uri = fromFacade("/content/notifications")
                .queryParam("since", "2014-10-15")
                .build();
        stubFor(WireMock.get(urlEqualTo(BASE_NOTIFICATION_PATH))
                .willReturn(aResponse()
                        .withBody(ALL_NOTIFICATIONS_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200))
        );
        final ClientResponse response = client.resource(uri)
                .header(HttpPipeline.POLICY_HEADER_NAME, Policy.INCLUDE_LAST_MODIFIED_DATE.getHeaderValue())
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(BASE_NOTIFICATION_PATH)));
            assertThat(response.getStatus(), is(200));
            String jsonPayload = response.getEntity(String.class);
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "lastModified"));
            assertThat(jsonPayload, not(containsNestedJsonProperty("notifications", "publishReference")));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "type"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "id"));
            assertThat(jsonPayload, containsNestedJsonProperty("notifications", "apiUrl"));
            assertThat(jsonPayload, containsJsonProperty("requestUrl"));
        } finally {
            response.close();
        }
    }

    private Matcher<? super String> containsNestedJsonProperty(final String property, final String nestedProperty) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("json property should be present: ").appendValue(property + "[i]." + nestedProperty);
            }

            @Override
            protected boolean matchesSafely(String jsonPayload) {
                Map<String, Object> notificationsResponse;
                try {
                    notificationsResponse = objectMapper.readValue(jsonPayload, JSON_MAP_TYPE);
                    List<Map<String, String>> notifications = (List) notificationsResponse.get(property);

                    for (Map<String, String> notification : notifications) {
                        if (notification.containsKey(nestedProperty)) {
                            return true;
                        }
                    }
                    return false;

                } catch (IOException e) {
                    return false;
                }
            }
        };
    }

    private void stubForRichContentWithYouTubeVideo() {
		stubFor(WireMock.get(urlPathEqualTo(CONTENT_PATH_2)).willReturn(
				aResponse()
						.withBody(RICH_CONTENT_JSON)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON)
						.withStatus(200)
		));
	}

    private String expectRequestUrl(ClientResponse response) throws IOException {
        Map<String, Object> result = expectOKResponseWithJSON(response);

        return (String) result.get("requestUrl");
    }

    private UriBuilder fromFacade(String path) {
        return UriBuilder.fromPath(path).host("localhost").port(policyComponent.getLocalPort()).scheme("http");
    }

    private UriBuilder fromFacade(String path, final Map<String, Object> queryParams) {
        final UriBuilder uriBuilder = UriBuilder.fromPath(path).host("localhost").port(policyComponent.getLocalPort()).scheme("http");
        for (String parameterName : queryParams.keySet()) {
            uriBuilder.queryParam(parameterName, queryParams.get(parameterName));
        }
        return uriBuilder;

    }

    private void assertWebUrl(Map<String, Object> result, String webUrl) {
        assertThat((String)result.get("webUrl"),is(webUrl));
    }

    private Map<String, Object> expectOKResponseWithJSON(ClientResponse response) throws IOException {
        assertThat(response.getStatus(), is(200));
        String bodyString = response.getEntity(String.class);

        return objectMapper.readValue(bodyString, JSON_MAP_TYPE);
    }

    private Matcher<? super String> containsJsonProperty(final String jsonProperty) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("json property should be present: ").appendValue(jsonProperty);
            }

            @Override
            protected boolean matchesSafely(String jsonPayload) {
                Map<String, Object> jsonMap;
                try {
                    jsonMap = objectMapper.readValue(jsonPayload, JSON_MAP_TYPE);
                } catch (IOException e) {
                    return false;
                }
                return jsonMap.containsKey(jsonProperty);
            }
        };
    }

    private int getLeasedConnections(String name){
        return client.resource("http://localhost:" + 21082).path("/metrics") //hardcoded because we have no access to getAdminPort() on the app rule
                .get(JsonNode.class)
                .get("gauges")
                .get("org.apache.http.conn.ClientConnectionManager." + name + ".leased-connections")
                        .get("value").asInt();

    }


}
