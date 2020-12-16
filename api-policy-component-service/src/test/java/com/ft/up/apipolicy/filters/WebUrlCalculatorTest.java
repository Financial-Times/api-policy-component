package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WebUrlCalculatorTest {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
  private static final byte[] WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE =
      ("{ \"identifiers\": [{\n"
              + "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n"
              + "\"identifierValue\": \"219512\"\n"
              + "}] }")
          .getBytes(UTF8);
  private static final byte[] WEB_URL_ELIGIBLE_IDENTIFIER_RESPONSE =
      ("{ \"identifiers\": [{\n"
              + "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n"
              + "\"identifierValue\": \"219512\"\n"
              + "}],\n"
              + "\"bodyXML\": \"<body>something here</body>\" }")
          .getBytes(UTF8);
  private static final byte[] WEB_URL_PRESENT_IN_RESPONSE =
      ("{ \"identifiers\": [{\n"
              + "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n"
              + "\"identifierValue\": \"219512\"\n"
              + "}],\n"
              + "\"type\": \"http://www.ft.com/ontology/content/Article\",\n"
              + "\"webUrl\": \"http://ftalphaville.ft.com/marketslive/2017-01-02/\" }")
          .getBytes(UTF8);
  private static final byte[] WEB_URL_ELIGIBLE_LIVE_BLOG_RESPONSE =
      ("{ \"identifiers\": [{\n"
              + "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n"
              + "\"identifierValue\": \"219512\"\n"
              + "}],\n"
              + "\"type\": \"http://www.ft.com/ontology/content/Article\",\n"
              + "\"realtime\": true }")
          .getBytes(UTF8);
  private static final byte[] WEB_URL_ELIGIBLE_NEXT_VIDEO_RESPONSE =
      ("{ \"identifiers\": [{\n"
              + "\"authority\": \"http://api.ft.com/system/NEXT-VIDEO-EDITOR\",\n"
              + "\"identifierValue\": \"219512\"\n"
              + "}],\n"
              + "\"type\": \"http://www.ft.com/ontology/content/MediaResource\" }")
          .getBytes(UTF8);
  private static final byte[] WEB_URL_ELIGIBLE_LIVE_BLOG_MULTI_TYPES_RESPONSE =
      ("{ \"identifiers\": [{\n"
              + "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n"
              + "\"identifierValue\": \"219512\"\n"
              + "}],\n"
              + "\"types\": [\"http://www.ft.com/ontology/content/Article\"],\n"
              + "\"realtime\": true }")
          .getBytes(UTF8);
  private static final String MINIMAL_PARTIAL_EXAMPLE_IDENTIFIER_RESPONSE =
      "{ \"identifiers\": [{\n"
          + "\"authority\": \"http://api.ft.com/system/FT-LABS-WP-1-91\",\n"
          + "\"identifierValue\": \"219512\"\n"
          + "}] }";
  private static final String NO_IDENTIFIERS_RESPONSE = "{ \"identifiers\": [] }";
  private static final Map<String, String> WEB_URL_TEMPLATES = new HashMap<>();

  @Mock private HttpPipelineChain mockChain;

  private WebUrlCalculator calculator =
      new WebUrlCalculator(WEB_URL_TEMPLATES, JsonConverter.testConverter());
  private MutableRequest exampleRequest =
      new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

  private MutableResponse exampleErrorResponse;
  private MutableResponse webUrlNonEligibleIdentifierResponse;
  private MutableResponse webUrlEligibleIdentifierResponse;
  private MutableResponse webUrlPresentInContentResponse;
  private MutableResponse originatingSystemIsNullResponse;
  private MutableResponse minimalPartialExampleResponse;

  @Before
  public void setUpExamples() {
    WEB_URL_TEMPLATES.put("http://www.ft.com/ontology/origin/FT-CLAMO", "TEST{{identifierValue}}");
    WEB_URL_TEMPLATES.put(
        "http://www.ft.com/ontology/origin/FT-LABS-WP-1-[0-9]+", "WP{{identifierValue}}");
    WEB_URL_TEMPLATES.put("http://api.ft.com/system/NEXT-VIDEO-EDITOR", "NVE{{identifierValue}}");

    exampleErrorResponse =
        new MutableResponse(new MultivaluedHashMap<>(), ERROR_RESPONSE.getBytes());
    exampleErrorResponse.setStatus(500);

    webUrlNonEligibleIdentifierResponse =
        new MutableResponse(new MultivaluedHashMap<>(), WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE);
    webUrlNonEligibleIdentifierResponse.setStatus(200);
    webUrlNonEligibleIdentifierResponse.getHeaders().putSingle("Content-Type", "application/json");

    minimalPartialExampleResponse =
        new MutableResponse(
            new MultivaluedHashMap<>(), MINIMAL_PARTIAL_EXAMPLE_IDENTIFIER_RESPONSE.getBytes());
    minimalPartialExampleResponse.setStatus(200);
    minimalPartialExampleResponse.getHeaders().putSingle("Content-Type", "application/json");

    webUrlEligibleIdentifierResponse =
        new MutableResponse(new MultivaluedHashMap<>(), WEB_URL_ELIGIBLE_IDENTIFIER_RESPONSE);
    webUrlEligibleIdentifierResponse.setStatus(200);
    webUrlEligibleIdentifierResponse.getHeaders().putSingle("Content-Type", "application/json");

    webUrlPresentInContentResponse =
        new MutableResponse(new MultivaluedHashMap<>(), WEB_URL_PRESENT_IN_RESPONSE);
    webUrlPresentInContentResponse.setStatus(200);
    webUrlPresentInContentResponse.getHeaders().putSingle("Content-Type", "application/json");

    originatingSystemIsNullResponse =
        new MutableResponse(new MultivaluedHashMap<>(), NO_IDENTIFIERS_RESPONSE.getBytes());
    originatingSystemIsNullResponse.setStatus(200);
    originatingSystemIsNullResponse.getHeaders().putSingle("Content-Type", "application/json");

    minimalPartialExampleResponse.getHeaders().putSingle("Content-Type", "application/json");
  }

  @Test
  public void shouldNotProcessErrorResponse() {
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(ERROR_RESPONSE.getBytes()));
  }

  @Test
  public void shouldNotProcessJSONLD() {
    webUrlNonEligibleIdentifierResponse
        .getHeaders()
        .putSingle("Content-Type", "application/ld-json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlNonEligibleIdentifierResponse);

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE));
  }

  @Test
  public void shouldNotAddWebUrlToSuccessResponseForNonEligibleWithIdentifiers() {
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlNonEligibleIdentifierResponse);

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE));
  }

  @Test
  public void shouldAddWebUrlToSuccessResponseForEligibleWithIdentifiers() {
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlEligibleIdentifierResponse);

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntityAsString(), containsString("\"webUrl\":\"TEST219512\""));
  }

  @Test
  public void shouldAddWebUrlToSuccessResponseForLiveBlog() {
    shouldAddWebUrlToSuccessResponse(WEB_URL_ELIGIBLE_LIVE_BLOG_RESPONSE, "TEST219512");
  }

  private void shouldAddWebUrlToSuccessResponse(
      byte[] successResponseWithEligibleWebUrl, final String expectedWebUrl) {
    MutableResponse liveBlogResponse =
        new MutableResponse(new MultivaluedHashMap<>(), successResponseWithEligibleWebUrl);
    liveBlogResponse.setStatus(200);
    liveBlogResponse.getHeaders().putSingle("Content-Type", "application/json");

    when(mockChain.callNextFilter(exampleRequest)).thenReturn(liveBlogResponse);

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(
        response.getEntityAsString(), containsString("\"webUrl\":\"" + expectedWebUrl + "\""));
  }

  @Test
  public void shouldAddWebUrlToSuccessResponseForLiveBlogEnrichedContent() {
    shouldAddWebUrlToSuccessResponse(WEB_URL_ELIGIBLE_LIVE_BLOG_MULTI_TYPES_RESPONSE, "TEST219512");
  }

  @Test
  public void shouldAddWebUrlToSuccessResponseForNextVideo() {
    shouldAddWebUrlToSuccessResponse(WEB_URL_ELIGIBLE_NEXT_VIDEO_RESPONSE, "NVE219512");
  }

  @Test
  public void shouldReturnSuccessResponseWithoutWebUrlWhenOriginatingSystemIsNull() {
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(originatingSystemIsNullResponse);

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntityAsString(), not(containsString("\"webUrl\":")));
  }

  @Test
  public void shouldAddWebUrlToSuccessResponseForRegexMatches() {
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalPartialExampleResponse);

    WebUrlCalculator calculator =
        new WebUrlCalculator(WEB_URL_TEMPLATES, JsonConverter.testConverter());

    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntityAsString(), not(containsString("\"webUrl\":\"WP219512\"")));
  }

  @Test
  public void shouldNotOverwriteWebUrlToSuccessResponseForContentWithDefaultWebUrl() {
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlPresentInContentResponse);
    MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(WEB_URL_PRESENT_IN_RESPONSE));
  }
}
