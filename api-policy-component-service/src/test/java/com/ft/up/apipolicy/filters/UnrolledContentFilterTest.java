package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.configuration.Policy.EXPAND_RICH_CONTENT;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UnrolledContentFilterTest {

  private UnrolledContentFilter filter;

  @Mock private MutableRequest request;

  @Mock private HttpPipelineChain chain;

  @Before
  public void setUp() {
    filter = new UnrolledContentFilter(INCLUDE_RICH_CONTENT, EXPAND_RICH_CONTENT);
  }

  @Test
  public void thatRequestParameterIsAppliedWhenRichContentPolicy() {
    when(request.policyIs(EXPAND_RICH_CONTENT)).thenReturn(true);
    when(request.policyIs(INCLUDE_RICH_CONTENT)).thenReturn(true);

    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).putSingle("unrollContent", "true");
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void thatNoChangeIsAppliedWhenNoValidPolicy() {
    filter.processRequest(request, chain);

    verify(request).policyIs(INCLUDE_RICH_CONTENT);
    verifyNoMoreInteractions(request);
    verify(chain).callNextFilter(request);
  }

  @Test
  public void thatNoChangeIsAppliedWhenOnlyExpandRichContentPolicyIsPresent() {
    when(request.policyIs(EXPAND_RICH_CONTENT)).thenReturn(Boolean.TRUE);
    filter.processRequest(request, chain);

    verify(request).policyIs(INCLUDE_RICH_CONTENT);
    verifyNoMoreInteractions(request);
    verify(chain).callNextFilter(request);
  }

  @Test
  public void thatNoChangeIsAppliedWhenOnlyIncludeRichContentPolicyIsPresent() {
    when(request.policyIs(INCLUDE_RICH_CONTENT)).thenReturn(Boolean.TRUE);
    filter.processRequest(request, chain);

    verify(request).policyIs(INCLUDE_RICH_CONTENT);
    verify(request).policyIs(EXPAND_RICH_CONTENT);
    verifyNoMoreInteractions(request);
    verify(chain).callNextFilter(request);
  }
}
