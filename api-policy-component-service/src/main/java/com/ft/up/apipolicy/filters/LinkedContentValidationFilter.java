package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class LinkedContentValidationFilter implements ApiFilter {

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    if (request.policyIs(Policy.INCLUDE_RICH_CONTENT)) {
      request.getQueryParameters().putSingle("validateLinkedResources", "true");
    }

    return chain.callNextFilter(request);
  }
}
