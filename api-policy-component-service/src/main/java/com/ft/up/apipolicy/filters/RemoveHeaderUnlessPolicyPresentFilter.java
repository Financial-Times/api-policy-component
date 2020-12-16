package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import javax.ws.rs.core.MultivaluedMap;

public class RemoveHeaderUnlessPolicyPresentFilter implements ApiFilter {

  private final String header;
  private final Policy policy;

  public RemoveHeaderUnlessPolicyPresentFilter(String header, Policy policy) {
    this.header = header;
    this.policy = policy;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    final MutableResponse response = chain.callNextFilter(request);
    if (response.getStatus() != 200) {
      return response;
    }

    MultivaluedMap<String, Object> headers = response.getHeaders();
    if (shouldFilterHeaderOut(request, response)) {
      headers.remove(header);
    }
    return response;
  }

  private boolean shouldFilterHeaderOut(MutableRequest request, MutableResponse response) {
    MultivaluedMap<String, Object> headers = response.getHeaders();
    return (!request.policyIs(policy) && headers.containsKey(header));
  }
}
