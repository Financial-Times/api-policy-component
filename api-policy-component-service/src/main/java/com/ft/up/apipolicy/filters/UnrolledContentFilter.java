package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class UnrolledContentFilter implements ApiFilter {

  private static final String UNROLL_CONTENT = "unrollContent";

  private final Policy[] policies;

  public UnrolledContentFilter(Policy... policies) {
    this.policies = policies;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    if (policies.length > 0 && shouldAddParameter(request)) {
      if (!request.getQueryParameters().containsKey(UNROLL_CONTENT)) {
        request.getQueryParameters().putSingle(UNROLL_CONTENT, Boolean.TRUE.toString());
      }
    }
    return chain.callNextFilter(request);
  }

  private boolean shouldAddParameter(MutableRequest request) {
    for (Policy policy : policies) {
      if (!request.policyIs(policy)) {
        return Boolean.FALSE;
      }
    }
    return Boolean.TRUE;
  }
}
