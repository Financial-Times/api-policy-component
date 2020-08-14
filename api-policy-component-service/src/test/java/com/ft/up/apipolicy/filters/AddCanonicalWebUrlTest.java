package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Collections;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddCanonicalWebUrlTest {

  @Mock private HttpPipelineChain mockChain;

  private AddCanonicalWebUrl addCanonicalWebUrl =
      new AddCanonicalWebUrl("https://www.ft.com/content/%s", JsonConverter.testConverter());
  private MutableRequest exampleRequest =
      new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

  @Test
  public void shouldNotProcessErrorResponse() {
    String entity = "{ \"message\" : \"Error\" }";
    MutableResponse errorResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    errorResponse.setStatus(500);
    errorResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(errorResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(entity.getBytes()));
  }

  @Test
  public void shouldNotProcessNonJsonResponse() {
    String entity = "Not a json";
    MutableResponse notAJsonResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    notAJsonResponse.setStatus(200);
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(notAJsonResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(entity.getBytes()));
  }

  @Test
  public void shouldNotProcessNonArticleNonVideoAndMissingBodyResponse() {
    String entity =
        "{ \"identifiers\": [{\n"
            + "\"authority\": \"http://www.ft.com/ontology/origin/FTComMethode\",\n"
            + "\"identifierValue\": \"ec1d36e6-f432-11e7-8715-e94187b3017e\"\n"
            + "}],\n"
            + "\"types\": [\"http://www.ft.com/ontology/content/not-an-article\"] }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(entity.getBytes()));
  }

  @Test
  public void shouldNotProcessResponseWithCanonicalWebUrl() {
    String entity =
        "{ \"canonicalWebUrl\" : \"canonical-web-url\", \"bodyXML\": \"<body>something here</body>\" }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(entity.getBytes()));
  }

  @Test
  public void shouldNotProcessResponseIfIdIsMissing() {
    String entity = "{ \"bodyXML\": \"<body>something here</body>\" }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(entity.getBytes()));
  }

  @Test
  public void shouldNotProcessResponseIfUuidIsMissingFromId() {
    String entity =
        "{ \"id\": \"http://www.ft.com/thing/missing-uuid\",\n"
            + "\"bodyXML\": \"<body>something here</body>\" }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(entity.getBytes()));
  }

  @Test
  public void shouldAddCanonicalWebUrlToVideos() {
    String entity =
        "{ \"id\": \"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\",\n"
            + "\"identifiers\": [{\n"
            + "\"authority\": \"http://api.ft.com/system/NEXT-VIDEO-EDITOR\",\n"
            + "\"identifierValue\": \"ec1d36e6-f432-11e7-8715-e94187b3017e\"\n"
            + "}] }";
    String expectedEntity =
        "{\"id\":\"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\","
            + "\"identifiers\":[{"
            + "\"authority\":\"http://api.ft.com/system/NEXT-VIDEO-EDITOR\","
            + "\"identifierValue\":\"ec1d36e6-f432-11e7-8715-e94187b3017e\""
            + "}],"
            + "\"canonicalWebUrl\":\"https://www.ft.com/content/ec1d36e6-f432-11e7-8715-e94187b3017e\"}";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(expectedEntity.getBytes()));
  }

  @Test
  public void shouldAddCanonicalWebUrlIfBodyIsPresent() {
    String entity =
        "{ \"id\": \"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\",\n"
            + "\"bodyXML\": \"<body>something here</body>\" }";
    String expectedEntity =
        "{\"id\":\"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\","
            + "\"bodyXML\":\"<body>something here</body>\","
            + "\"canonicalWebUrl\":\"https://www.ft.com/content/ec1d36e6-f432-11e7-8715-e94187b3017e\"}";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(expectedEntity.getBytes()));
  }

  @Test
  public void shouldAddCanonicalWebUrlToArticleType() {
    String entity =
        "{ \"id\": \"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\",\n"
            + "\"type\": \"http://www.ft.com/ontology/content/Article\" }";
    String expectedEntity =
        "{\"id\":\"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\","
            + "\"type\":\"http://www.ft.com/ontology/content/Article\","
            + "\"canonicalWebUrl\":\"https://www.ft.com/content/ec1d36e6-f432-11e7-8715-e94187b3017e\"}";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(expectedEntity.getBytes()));
  }

  @Test
  public void shouldAddCanonicalWebUrlWhenTypesContainsArticleType() {
    String entity =
        "{ \"id\": \"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\",\n"
            + "\"types\": [\"http://www.ft.com/ontology/content/Article\"] }";
    String expectedEntity =
        "{\"id\":\"http://www.ft.com/thing/ec1d36e6-f432-11e7-8715-e94187b3017e\","
            + "\"types\":[\"http://www.ft.com/ontology/content/Article\"],"
            + "\"canonicalWebUrl\":\"https://www.ft.com/content/ec1d36e6-f432-11e7-8715-e94187b3017e\"}";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    when(mockChain.callNextFilter(exampleRequest)).thenReturn(validResponse);

    MutableResponse response = addCanonicalWebUrl.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(expectedEntity.getBytes()));
  }
}
