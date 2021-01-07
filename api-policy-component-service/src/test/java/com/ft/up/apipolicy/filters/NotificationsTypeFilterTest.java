package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Arrays;
import java.util.Collections;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationsTypeFilterTest {

  public static final String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
  public static final String SUCCESS_RESPONSE =
      "{ \"requestUrl\":"
          + " \"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource\","
          + " \"links\": [ {\"href\":"
          + " \"http://example.org/content/100?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource\","
          + " \"rel\" : \"next\"}] }";
  public static final String STRIPPED_SUCCESS_RESPONSE =
      "{\"requestUrl\":\"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z\",\"links\":[{\"href\":\"http://example.org/content/100?since=2016-07-23T00:00:00.000Z\",\"rel\":\"next\"}]}";

  private final JsonConverter jsonConverter = JsonConverter.testConverter();

  private NotificationsTypeFilter filter =
      new NotificationsTypeFilter(jsonConverter, Policy.INTERNAL_UNSTABLE);
  private MutableRequest request = mock(MutableRequest.class);
  private HttpPipelineChain chain = mock(HttpPipelineChain.class);
  private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

  private MutableResponse errorResponse;
  private MutableResponse successResponse;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    errorResponse = new MutableResponse(headers, ERROR_RESPONSE.getBytes());
    errorResponse.setStatus(500);

    successResponse = new MutableResponse(headers, SUCCESS_RESPONSE.getBytes());
    successResponse.setStatus(200);
  }

  @Test
  public void testThatArticleTypeQueryParamIsAddedWhenNoPolicyIsPresent() throws Exception {
    when(request.policyIs(Policy.INCLUDE_PROVENANCE)).thenReturn(false);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).put("type", Collections.singletonList("article"));
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void testThatMediaResourceTypeQueryParamIsAddedWhenPolicyIsPresent() throws Exception {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).put("type", Arrays.asList("all"));
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void testIncomingQueryParamsCannotOverwritePolicyRestriction() throws Exception {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(false);
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.putSingle("type", "mediaResource");
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    verify(chain).callNextFilter(request);
    assertThat(params.get("type"), equalTo(Collections.singletonList("article")));
  }

  @Test
  public void testThatMonitorQueryParamIsSetToTrueWhenPolicyIsPresent() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    verify(params).putSingle("monitor", "true");
  }

  @Test
  public void testThatMonitorQueryParamIsSetToFalseWhenPolicyIsNotPresent() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(false);
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    verify(params).putSingle("monitor", "false");
  }

  @Test
  public void testThatMonitorQueryParamCannotOverwritePolicyRestriction() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(false);
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.putSingle("monitor", "true");
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    assertEquals("false", params.getFirst("monitor"));
  }

  @Test
  public void testThatForNon200ResponseNoOtherInteractionHappens() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(errorResponse);

    filter.processRequest(request, chain);

    verify(request).policyIs(Policy.INTERNAL_UNSTABLE);
    verify(request, times(2)).getQueryParameters();
    verifyNoMoreInteractions(request);
  }

  @Test
  public void testThatFilterExceptionIsThrownWhenUnexpectedJSONFieldTypes() {
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);

    MutableResponse badResponse = new MutableResponse(headers, "{ \"requestUrl\": [] }".getBytes());
    badResponse.setStatus(200);
    when(chain.callNextFilter(request)).thenReturn(badResponse);

    expectedException.expect(FilterException.class);

    filter.processRequest(request, chain);
  }

  @Test
  public void testThatForResponseWithEmptyLinksArrayTypeParamsInURLsAreStripped() {
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);

    String responseBody =
        "{ \"requestUrl\":"
            + " \"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource\","
            + " \"links\": [] }";
    String strippedBody =
        "{\"requestUrl\":\"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z\",\"links\":[]}";
    MutableResponse responseWithEmptyLinksArray =
        new MutableResponse(headers, responseBody.getBytes());
    responseWithEmptyLinksArray.setStatus(200);
    when(chain.callNextFilter(request)).thenReturn(responseWithEmptyLinksArray);

    MutableResponse returned = filter.processRequest(request, chain);

    assertThat("", returned.getEntityAsString(), is(strippedBody));
  }

  @Test
  public void testThatForHappyResponseTypeParamsInURLsAreStripped() {
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);

    MutableResponse happyResponse = new MutableResponse(headers, SUCCESS_RESPONSE.getBytes());
    happyResponse.setStatus(200);
    when(chain.callNextFilter(request)).thenReturn(happyResponse);

    MutableResponse returned = filter.processRequest(request, chain);

    assertThat("", returned.getEntityAsString(), is(STRIPPED_SUCCESS_RESPONSE));
  }
}
