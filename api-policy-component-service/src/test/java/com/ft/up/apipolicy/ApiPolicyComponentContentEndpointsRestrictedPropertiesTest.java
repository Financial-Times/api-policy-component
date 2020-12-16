package com.ft.up.apipolicy;

import static com.ft.up.apipolicy.JsonConverter.JSON_MAP_TYPE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.ConfigOverride.config;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Parameterized JUnit test for properties that are filtered for the content, preview, and enriched
 * content endpoints.
 */
@RunWith(Parameterized.class)
public class ApiPolicyComponentContentEndpointsRestrictedPropertiesTest
    extends AbstractApiComponentTest {
  private static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";
  private static final String ENRICHED_CONTENT_PATH =
      "/enrichedcontent/bcafca32-5bc7-343f-851f-fd6d3514e694";
  private static final String INTERNAL_CONTENT_PATH =
      "/internalcontent/bcafca32-5bc7-343f-851f-fd6d3514e694";

  private static final String EDITORIAL_DESK = "/FT/TestDesk";
  private static final String INTERNAL_ANALYTICS_TAGS = "some-tag, another-tag";

  @ClassRule
  public static final DropwizardAppRule<ApiPolicyConfiguration> policyComponent =
      new DropwizardAppRule<>(
          ApiPolicyApplication.class,
          resourceFilePath("config-junit.yml"),
          config("varnish.primaryNodes", primaryNodes));

  private static final String CONTENT_PREVIEW_PATH =
      "/content-preview/bcafca32-5bc7-343f-851f-fd6d3514e694";

  private static final String ARTICLE_JSON =
      "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", "
          + "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\",\n"
          + "\"openingXML\" : \"<body>a video</body>\",\n"
          + "\"alternativeTitles\" : {},\n"
          + "\"alternativeImages\" : {},\n"
          + "\"alternativeStandfirsts\" : {},\n"
          + "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n"
          + "\"publishReference\": \"tid_junit_publishref\",\n"
          + "\"identifiers\": [{\n"
          + "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n"
          + "\"identifierValue\": \"220322\"\n"
          + "}],\n"
          + "\"mainImage\": {"
          + "\"id\": \"http://www.ft.com/content/273563f3-95a0-4f00-8966-6973c0111923\""
          + "},\n"
          + "\"comments\": {\n"
          + "\"enabled\": true\n"
          + "},\n"
          + "\"editorialDesk\":\""
          + EDITORIAL_DESK
          + "\", \n"
          + "\"internalAnalyticsTags\":\""
          + INTERNAL_ANALYTICS_TAGS
          + "\"";

  private static final String CONTENT_JSON = "{" + ARTICLE_JSON + "}";

  private static final String ENRICHED_CONTENT_JSON =
      "{" + ARTICLE_JSON + ",\n" + "\"brands\": [ ],\n" + "\"annotations\": [ ]" + "}";

  private static final String INTERNAL_CONTENT_JSON = ENRICHED_CONTENT_JSON;

  private static final String IMAGE_JSON =
      "{"
          + "\"id\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\","
          + "\"type\":\"http://www.ft.com/ontology/content/MediaResource\","
          + "\"apiUrl\":\"http://api.ft.com/content/5991fb44-f1eb-11e6-95ee-f14e55513608\","
          + "\"publishedDate\":\"2015-02-03T12:58:00.000Z\","
          + "\"lastModified\":\"2017-02-13T17:51:57.723Z\","
          + "\"canBeSyndicated\":\"verify\","
          + "\"masterSource\": {\n"
          + "        \"authority\": \"http://api.ft.com/system/FT-FOTOWARE\",\n"
          + "        \"identifierValue\": \"1234_FOTOWARE_ID\"\n"
          + "    }";

  private static final String IMAGE_CONTENT_JSON = IMAGE_JSON + "}";

  private static final String IMAGE_ENRICHED_CONTENT_JSON =
      IMAGE_JSON + ",\n" + "\"brands\": [ ],\n" + "\"annotations\": [ ]" + "}";

  private static final String IMAGE_INTERNAL_CONTENT_JSON = IMAGE_ENRICHED_CONTENT_JSON;

  @Parameters
  public static Collection<Object[]> data() {
    /* Elements in each array are as follows:
     * - Property name
     * - Required policy
     * - Expect value to be present in /content endpoint when allowed by policy
     * - Expect value to be present in /content-preview endpoint when allowed by policy
     * - Expect value to be present in /enrichedcontent endpoint when allowed by policy
     * - Expect value to be present in /internalcontent endpoint when allowed by policy
     */
    return Arrays.asList(
        new Object[][] {
          {
            "comments",
            Policy.INCLUDE_COMMENTS,
            false,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "identifiers",
            Policy.INCLUDE_IDENTIFIERS,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "lastModified",
            Policy.INCLUDE_LAST_MODIFIED_DATE,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "publishReference",
            Policy.INCLUDE_PROVENANCE,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "masterSource",
            Policy.INCLUDE_PROVENANCE,
            true,
            true,
            true,
            true,
            IMAGE_CONTENT_JSON,
            IMAGE_ENRICHED_CONTENT_JSON,
            IMAGE_INTERNAL_CONTENT_JSON
          },
          {
            "mainImage",
            Policy.INCLUDE_RICH_CONTENT,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "openingXML",
            Policy.INTERNAL_UNSTABLE,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "alternativeTitles",
            Policy.INTERNAL_UNSTABLE,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "alternativeImages",
            Policy.INTERNAL_UNSTABLE,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "alternativeStandfirsts",
            Policy.INTERNAL_UNSTABLE,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "editorialDesk",
            Policy.INTERNAL_ANALYTICS,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          },
          {
            "internalAnalyticsTags",
            Policy.INTERNAL_ANALYTICS,
            true,
            true,
            true,
            true,
            CONTENT_JSON,
            ENRICHED_CONTENT_JSON,
            INTERNAL_CONTENT_JSON
          }
        });
  }

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  @Rule public final WireMockClassRule wireMockForVarnish = WIRE_MOCK_1;

  private final String propertyName;
  private final Policy requiredPolicy;
  private final boolean allowForContentEndpoint;
  private final boolean allowForContentPreviewEndpoint;
  private final boolean allowForEnrichedContentEndpoint;
  private final boolean allowForInternalContentEndpoint;
  private final String jsonResponseForContent;
  private final String jsonResponseForEnrichedContent;
  private final String jsonResponseForInternalContent;

  private final Client client = JerseyClientBuilder.newClient();
  private Response response;

  public ApiPolicyComponentContentEndpointsRestrictedPropertiesTest(
      String propertyName,
      Policy requiredPolicy,
      boolean allowForContent,
      boolean allowForContentPreview,
      boolean allowForEnrichedContent,
      boolean allowForInternalContent,
      String jsonResponseForContent,
      String jsonResponseForEnrichedContent,
      String jsonResponseForInternalContent) {

    this.propertyName = propertyName;
    this.requiredPolicy = requiredPolicy;
    this.allowForContentEndpoint = allowForContent;
    this.allowForContentPreviewEndpoint = allowForContentPreview;
    this.allowForEnrichedContentEndpoint = allowForEnrichedContent;
    this.allowForInternalContentEndpoint = allowForInternalContent;
    this.jsonResponseForContent = jsonResponseForContent;
    this.jsonResponseForEnrichedContent = jsonResponseForEnrichedContent;
    this.jsonResponseForInternalContent = jsonResponseForInternalContent;
  }

  @Before
  public void setUp() {
    stubFor(
        WireMock.get(urlPathEqualTo(CONTENT_PATH))
            .willReturn(
                aResponse()
                    .withBody(jsonResponseForContent)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(SC_OK)));
    stubFor(
        WireMock.get(urlPathEqualTo(CONTENT_PREVIEW_PATH))
            .willReturn(
                aResponse()
                    .withBody(jsonResponseForContent)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(200)));
    stubFor(
        WireMock.get(urlPathEqualTo(ENRICHED_CONTENT_PATH))
            .willReturn(
                aResponse()
                    .withBody(jsonResponseForEnrichedContent)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(200)));
    stubFor(
        WireMock.get(urlPathEqualTo(INTERNAL_CONTENT_PATH))
            .willReturn(
                aResponse()
                    .withBody(jsonResponseForInternalContent)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(200)));
  }

  @After
  public void tearDown() {
    if (response != null) response.close();
  }

  private UriBuilder fromFacade(String path) {
    return UriBuilder.fromPath(path)
        .host("localhost")
        .port(policyComponent.getLocalPort())
        .scheme("http");
  }

  private void checkResponse(String path, boolean expectProperty) {
    verify(getRequestedFor(urlPathEqualTo(path)));
    assertThat(response.getStatus(), is(SC_OK));
    if (expectProperty) {
      assertThat(response.readEntity(String.class), containsJsonProperty(propertyName));
    } else {
      assertThat(response.readEntity(String.class), not(containsJsonProperty(propertyName)));
    }
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
          jsonMap = OBJECT_MAPPER.readValue(jsonPayload, JSON_MAP_TYPE);
        } catch (IOException e) {
          return false;
        }
        return jsonMap.containsKey(jsonProperty);
      }
    };
  }

  @Test
  public void shouldAllowPropertyForContentWhenPolicyIsPresent() throws Exception {
    final URI uri = fromFacade(CONTENT_PATH).build();
    response =
        client
            .target(uri)
            .request()
            .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
            .get();

    checkResponse(CONTENT_PATH, allowForContentEndpoint);
  }

  @Test
  public void shouldAllowPropertyForContentPreviewWhenPolicyIsPresent() throws Exception {
    final URI uri = fromFacade(CONTENT_PREVIEW_PATH).build();
    response =
        client
            .target(uri)
            .request()
            .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
            .get();

    checkResponse(CONTENT_PREVIEW_PATH, allowForContentPreviewEndpoint);
  }

  @Test
  public void shouldAllowPropertyForEnrichedContentWhenPolicyIsPresent() throws Exception {
    final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
    response =
        client
            .target(uri)
            .request()
            .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
            .get();

    checkResponse(ENRICHED_CONTENT_PATH, allowForEnrichedContentEndpoint);
  }

  @Test
  public void shouldAllowPropertyForInternalContentWhenPolicyIsPresent() throws Exception {
    final URI uri = fromFacade(INTERNAL_CONTENT_PATH).build();
    response =
        client
            .target(uri)
            .request()
            .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
            .get();

    checkResponse(INTERNAL_CONTENT_PATH, allowForInternalContentEndpoint);
  }

  @Test
  public void shouldRemovePropertyForContentWhenPolicyIsNotPresent() throws Exception {
    final URI uri = fromFacade(CONTENT_PATH).build();
    response = client.target(uri).request().get();

    checkResponse(CONTENT_PATH, false);
  }

  @Test
  public void shouldRemovePropertyForContentPreviewWhenPolicyIsNotPresent() throws Exception {
    final URI uri = fromFacade(CONTENT_PREVIEW_PATH).build();
    response = client.target(uri).request().get();

    checkResponse(CONTENT_PREVIEW_PATH, false);
  }

  @Test
  public void shouldRemovePropertyForEnrichedContentWhenPolicyIsNotPresent() throws Exception {
    final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
    response = client.target(uri).request().get();

    checkResponse(ENRICHED_CONTENT_PATH, false);
  }

  @Test
  public void shouldRemovePropertyForInternalContentWhenPolicyIsNotPresent() throws Exception {
    final URI uri = fromFacade(INTERNAL_CONTENT_PATH).build();
    response = client.target(uri).request().get();

    checkResponse(INTERNAL_CONTENT_PATH, false);
  }
}
