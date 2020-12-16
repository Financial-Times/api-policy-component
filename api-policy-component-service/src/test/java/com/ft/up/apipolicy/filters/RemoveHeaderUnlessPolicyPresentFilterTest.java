package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.configuration.Policy.INTERNAL_UNSTABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemoveHeaderUnlessPolicyPresentFilterTest {

  private static final String ACCESS_LEVEL_HEADER = "X-FT-Access-Level";

  private RemoveHeaderUnlessPolicyPresentFilter removeHeaderUnlessPolicyPresentFilter;

  @Before
  public void setUp() {
    removeHeaderUnlessPolicyPresentFilter =
        new RemoveHeaderUnlessPolicyPresentFilter(ACCESS_LEVEL_HEADER, Policy.INTERNAL_UNSTABLE);
  }

  @Test
  public void testFiltersHeader() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Content-Type", MediaType.APPLICATION_JSON);
    headers.add(ACCESS_LEVEL_HEADER, "subscribed");
    final MutableResponse initialResponse = new MutableResponse(headers, new byte[0]);
    initialResponse.setStatus(200);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

    final MutableResponse processedResponse =
        removeHeaderUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    assertNull(processedResponse.getHeaders().getFirst(ACCESS_LEVEL_HEADER));
  }

  @Test
  public void testLeavesHeaderWhenPolicyHeaderSet() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    when(mockedRequest.policyIs(INTERNAL_UNSTABLE)).thenReturn(true);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Content-Type", MediaType.APPLICATION_JSON);
    headers.add(ACCESS_LEVEL_HEADER, "subscribed");
    final MutableResponse initialResponse = new MutableResponse(headers, new byte[0]);
    initialResponse.setStatus(200);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

    final MutableResponse processedResponse =
        removeHeaderUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    assertEquals(processedResponse.getHeaders().getFirst(ACCESS_LEVEL_HEADER), "subscribed");
  }

  @Test
  public void testDoesNotTouchWhenHeaderIsAbsent() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Content-Type", MediaType.APPLICATION_JSON);
    final MutableResponse initialResponse = new MutableResponse(headers, new byte[0]);
    initialResponse.setStatus(200);
    final MutableResponse spiedResponse = spy(initialResponse);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(spiedResponse);

    removeHeaderUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    verifyResponseNotMutated(spiedResponse);
  }

  @Test
  public void testLeavesEverythingWhenNot200() throws Exception {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
    final MutableResponse mockedResponse = mock(MutableResponse.class);
    when(mockedResponse.getStatus()).thenReturn(422);
    when(mockedChain.callNextFilter(mockedRequest)).thenReturn(mockedResponse);

    removeHeaderUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

    verifyResponseNotMutated(mockedResponse);
  }

  public void verifyResponseNotMutated(final MutableResponse mockedResponse) {
    verify(mockedResponse, never()).setEntity(Mockito.any(byte[].class));
    verify(mockedResponse, never()).setHeaders(Mockito.any(MultivaluedMap.class));
    verify(mockedResponse, never()).setStatus(anyInt());
  }
}
