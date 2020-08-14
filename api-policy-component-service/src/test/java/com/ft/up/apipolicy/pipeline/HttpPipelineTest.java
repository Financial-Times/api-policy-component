package com.ft.up.apipolicy.pipeline;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.Mockito.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * HttpPipelineTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpPipelineTest {

  public static final String SYNTHETIC_EXCEPTION_MESSAGE = "Synthetic exception";
  @Mock RequestForwarder forwarder;

  @Mock private MutableRequest request;

  @Rule public org.junit.rules.ExpectedException expectedException = ExpectedException.none();

  @Test
  public void successfulRequestShouldPropagateToForwarder() {

    ApiFilter filter1 = new DummyFilter();
    ApiFilter filter2 = new DummyFilter();

    HttpPipelineChain chain = makeChain(filter1, filter2);

    chain.callNextFilter(request);

    verify(forwarder, times(1)).forwardRequest(request);
  }

  @Test
  public void successfulRequestShouldTouchAllTheFilters() {

    ApiFilter filter1 = spy(new DummyFilter());
    ApiFilter filter2 = spy(new DummyFilter());

    HttpPipelineChain chain = makeChain(filter1, filter2);

    chain.callNextFilter(request);

    verify(filter1, times(1)).processRequest(request, chain);
    verify(filter2, times(1)).processRequest(request, chain);
  }

  @Test
  public void requestFailingBeforeForwarderCallShouldNotPropagateToForwarder() {
    ApiFilter filter1 = spy(new DummyFilter());
    ApiFilter filter2 = spy(new DummyFilter());

    HttpPipelineChain chain = makeChain(filter1, filter2);

    doThrow(new RuntimeException("Synthetic exception"))
        .when(filter2)
        .processRequest(request, chain);

    try {
      chain.callNextFilter(request);
    } catch (RuntimeException e) {
      // ok
    }

    verify(filter1, times(1)).processRequest(request, chain);
    verify(filter2, times(1)).processRequest(request, chain);
    verify(forwarder, never()).forwardRequest(request);
  }

  @Test
  public void requestFailingInFirstFilterDoesNotCallSecondFilter() {
    ApiFilter filter1 = spy(new DummyFilter());
    ApiFilter filter2 = spy(new DummyFilter());

    HttpPipelineChain chain = makeChain(filter1, filter2);

    doThrow(new RuntimeException(SYNTHETIC_EXCEPTION_MESSAGE))
        .when(filter1)
        .processRequest(request, chain);

    try {
      chain.callNextFilter(request);
    } catch (RuntimeException e) {
      // ok
    }

    verify(filter1, times(1)).processRequest(request, chain);
    verify(filter2, never()).processRequest(request, chain);
    verify(forwarder, never()).forwardRequest(request);
  }

  @Test
  public void filterExceptionsShouldBubbledToInvokingMethod() {
    ApiFilter filter1 = new DummyFilter();
    ApiFilter filter2 = spy(new DummyFilter());

    HttpPipelineChain chain = makeChain(filter1, filter2);

    doThrow(new RuntimeException(SYNTHETIC_EXCEPTION_MESSAGE))
        .when(filter2)
        .processRequest(request, chain);

    expectedException.expect(RuntimeException.class);
    expectedException.expect(hasProperty("message", equalTo(SYNTHETIC_EXCEPTION_MESSAGE)));

    chain.callNextFilter(request);
  }

  private HttpPipelineChain makeChain(ApiFilter filter1, ApiFilter filter2) {
    HttpPipeline pipeline = new HttpPipeline(forwarder, filter1, filter2);

    return new HttpPipelineChain(pipeline);
  }
}
