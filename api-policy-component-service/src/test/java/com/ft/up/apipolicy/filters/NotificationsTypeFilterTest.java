package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationsTypeFilterTest {

  private static final String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
  private static final String SUCCESS_RESPONSE =
      "{ \"requestUrl\":"
          + " \"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource&monitor=false\","
          + " \"links\": [ {\"href\":"
          + " \"http://example.org/content/100?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource&monitor=false\","
          + " \"rel\" : \"next\"}] }";
  private static final String STRIPPED_SUCCESS_RESPONSE =
      "{\"requestUrl\":\"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z\",\"links\":[{\"href\":\"http://example.org/content/100?since=2016-07-23T00:00:00.000Z\",\"rel\":\"next\"}]}";

  private final JsonConverter jsonConverter = JsonConverter.testConverter();

  private final NotificationsTypeFilter filter = new NotificationsTypeFilter(jsonConverter);
  private final MutableRequest request = mock(MutableRequest.class);
  private final HttpPipelineChain chain = mock(HttpPipelineChain.class);
  private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

  private MutableResponse errorResponse;
  private MutableResponse successResponse;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Mock private MultivaluedMap<String, String> params;

  @Before
  public void setUp() {
    errorResponse = new MutableResponse(headers, ERROR_RESPONSE.getBytes());
    errorResponse.setStatus(500);

    successResponse = new MutableResponse(headers, SUCCESS_RESPONSE.getBytes());
    successResponse.setStatus(200);
  }

  @Test
  public void testThatArticleTypeQueryParamIsAddedWhenNoPolicyIsPresent() {
    when(request.policyIs(Policy.INCLUDE_PROVENANCE)).thenReturn(false);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).put("type", Collections.singletonList("Article"));
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void testThatMediaResourceTypeQueryParamIsAddedWhenPolicyIsPresent() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).put("type", Collections.singletonList("All"));
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void testThatExtendedTypesAreAddedWhenPolicyIsPresent() {
    when(request.policyIs(Policy.EXTENDED_PULL_NOTIFICATIONS)).thenReturn(true);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).put("type", Arrays.asList("Article", "LiveBlogPackage", "LiveBlogPost"));
    inOrder.verify(params).put("monitor", Collections.singletonList("false"));
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void testIncomingQueryParamsCannotOverwritePolicyRestriction() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(false);
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.putSingle("type", "mediaResource");
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    verify(chain).callNextFilter(request);
    assertThat(params.get("type"), equalTo(Collections.singletonList("Article")));
  }

  @Test
  public void testThatMonitorQueryParamIsSetToTrueWhenPolicyIsPresent() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    verify(params).put("monitor", Collections.singletonList("true"));
  }

  @Test
  public void testThatMonitorQueryParamIsSetToFalseWhenPolicyIsNotPresent() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(false);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    verify(params).put("monitor", Collections.singletonList("false"));
  }

  @Test
  public void testThatMonitorQueryParamCannotOverwritePolicyRestriction() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(false);
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.putSingle("monitor", "true");
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(successResponse);

    filter.processRequest(request, chain);

    assertThat(params.get("monitor"), equalTo(Collections.singletonList("false")));
  }

  @Test
  public void testThatForNon200ResponseNoOtherInteractionHappens() {
    when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
    when(request.getQueryParameters()).thenReturn(params);
    when(chain.callNextFilter(request)).thenReturn(errorResponse);

    filter.processRequest(request, chain);

    verify(request).policyIs(Policy.INTERNAL_UNSTABLE);
    verify(request, times(2)).getQueryParameters();
    verifyNoMoreInteractions(request);
  }

  @Test
  public void testThatFilterExceptionIsThrownWhenUnexpectedJSONFieldTypes() {
    when(request.getQueryParameters()).thenReturn(params);

    MutableResponse badResponse = new MutableResponse(headers, "{ \"requestUrl\": [] }".getBytes());
    badResponse.setStatus(200);
    when(chain.callNextFilter(request)).thenReturn(badResponse);

    expectedException.expect(FilterException.class);

    filter.processRequest(request, chain);
  }

  @Test
  public void testThatForResponseWithEmptyLinksArrayTypeParamsInURLsAreStripped() {
    when(request.getQueryParameters()).thenReturn(params);

    String responseBody =
        "{ \"requestUrl\":"
            + " \"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource&monitor=false\","
            + " \"links\": [] }";
    String strippedBody =
        "{\"requestUrl\":\"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z\",\"links\":[]}";
    MutableResponse responseWithEmptyLinksArray =
        new MutableResponse(headers, responseBody.getBytes());
    responseWithEmptyLinksArray.setStatus(200);
    when(chain.callNextFilter(request)).thenReturn(responseWithEmptyLinksArray);

    MutableResponse returned = filter.processRequest(request, chain);

    assertThat(returned.getEntityAsString(), is(strippedBody));
  }

  @Test
  public void testThatForHappyResponseTypeParamsInURLsAreStripped() {
    when(request.getQueryParameters()).thenReturn(params);

    MutableResponse happyResponse = new MutableResponse(headers, SUCCESS_RESPONSE.getBytes());
    happyResponse.setStatus(200);
    when(chain.callNextFilter(request)).thenReturn(happyResponse);

    MutableResponse returned = filter.processRequest(request, chain);

    assertThat(returned.getEntityAsString(), is(STRIPPED_SUCCESS_RESPONSE));
  }
}
