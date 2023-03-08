package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_LAST_MODIFIED_DATE;
import static com.ft.up.apipolicy.pipeline.ApiFilter.ALTERNATIVE_IMAGES;
import static com.ft.up.apipolicy.pipeline.ApiFilter.EMBEDS;
import static com.ft.up.apipolicy.pipeline.ApiFilter.MAIN_IMAGE;
import static com.ft.up.apipolicy.pipeline.ApiFilter.MEMBERS;
import static com.ft.up.apipolicy.pipeline.ApiFilter.PROMOTIONAL_IMAGE;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemoveJsonPropertiesUnlessPolicyPresentFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonConverter jsonConverter = new JsonConverter(objectMapper);
  private final RemoveJsonPropertiesUnlessPolicyPresentFilter
      removeJsonPropertyUnlessPolicyPresentFilter =
          new RemoveJsonPropertiesUnlessPolicyPresentFilter(
              jsonConverter,
              Collections.singleton(INCLUDE_LAST_MODIFIED_DATE),
              "lastModified",
              "comments");

  private static byte[] readFileBytes(final String path) {
    try {
      return Files.readAllBytes(
          Paths.get(
              RemoveJsonPropertiesUnlessPolicyPresentFilterTest.class
                  .getClassLoader()
                  .getResource(path)
                  .toURI()));
    } catch (IOException | URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  public void testFiltersJsonProperty() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Content-Type", MediaType.APPLICATION_JSON);
    final MutableResponse initialResponse =
        new MutableResponse(headers, readFileBytes("sample-article-with-image.json"));
    initialResponse.setStatus(200);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

    final MutableResponse processedResponse =
        removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    final JsonNode actualTree = objectMapper.readTree(processedResponse.getEntityAsString());
    assertFalse(actualTree.has("lastModified"));
    assertFalse(actualTree.has("comments"));

    final JsonNode mainImageTree = actualTree.get(MAIN_IMAGE);
    assertFalse(mainImageTree.has("lastModified"));

    final JsonNode mainImageMember = mainImageTree.get(MEMBERS).get(0);
    assertFalse(mainImageMember.has("lastModified"));

    final JsonNode embeddedImageSet = actualTree.get(EMBEDS).get(0);
    assertFalse(embeddedImageSet.has("lastModified"));

    final JsonNode embeddedImage = embeddedImageSet.get(MEMBERS).get(0);
    assertFalse(embeddedImage.has("lastModified"));

    final JsonNode promotionalImage = actualTree.get(ALTERNATIVE_IMAGES).get(PROMOTIONAL_IMAGE);
    assertFalse(promotionalImage.has("lastModified"));
  }

  @Test
  public void testLeavesJsonPropertyWhenPolicyHeaderSet() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    when(mockedRequest.policyIs(INCLUDE_LAST_MODIFIED_DATE)).thenReturn(true);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Content-Type", MediaType.APPLICATION_JSON);
    byte[] body = readFileBytes("sample-article-with-image.json");
    final MutableResponse initialResponse = new MutableResponse(headers, body);
    initialResponse.setStatus(200);
    final MutableResponse spiedResponse = spy(initialResponse);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(spiedResponse);

    removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    verifyResponseNotMutated(spiedResponse);
  }

  @Test
  public void testDoesntTouchWhenJsonPropertyIsAbsent() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Content-Type", MediaType.APPLICATION_JSON);
    byte[] body = readFileBytes("sample-article-no-last-modified.json");
    final MutableResponse initialResponse = new MutableResponse(headers, body);
    initialResponse.setStatus(200);
    final MutableResponse spiedResponse = spy(initialResponse);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(spiedResponse);

    removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    verifyResponseNotMutated(spiedResponse);
  }

  @Test
  public void testLeavesEverythingWhenNot200() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MutableResponse mockedResponse = mock(MutableResponse.class);
    when(mockedResponse.getStatus()).thenReturn(400);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(mockedResponse);

    removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    verifyResponseNotMutated(mockedResponse);
  }

  public void verifyResponseNotMutated(final MutableResponse mockedResponse) {
    verify(mockedResponse, never()).setEntity(Mockito.any(byte[].class));
    verify(mockedResponse, never()).setHeaders(Mockito.any(MultivaluedMap.class));
    verify(mockedResponse, never()).setStatus(anyInt());
  }
}
