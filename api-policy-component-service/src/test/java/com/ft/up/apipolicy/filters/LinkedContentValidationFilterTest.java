package com.ft.up.apipolicy.filters;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.mockito.InOrder;

public class LinkedContentValidationFilterTest {
  private LinkedContentValidationFilter filter = new LinkedContentValidationFilter();
  private MutableRequest request = mock(MutableRequest.class);
  private HttpPipelineChain chain = mock(HttpPipelineChain.class);

  @Test
  public void thatRequestParameterIsAppliedWhenRichContentPolicy() {
    when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(true);
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
    when(request.getQueryParameters()).thenReturn(params);
    filter.processRequest(request, chain);

    InOrder inOrder = inOrder(chain, params);
    inOrder.verify(params).putSingle("validateLinkedResources", "true");
    inOrder.verify(chain).callNextFilter(request);
  }

  @Test
  public void thatNoChangeIsAppliedWhenNotRichContentPolicy() {
    filter.processRequest(request, chain);

    verify(request).policyIs(Policy.INCLUDE_RICH_CONTENT);
    verifyNoMoreInteractions(request);
    verify(chain).callNextFilter(request);
  }
}
