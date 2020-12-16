package com.ft.up.apipolicy.pipeline;

/**
 * DummyFilter
 *
 * @author Simon.Gibbs
 */
public class DummyFilter implements ApiFilter {
  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    return chain.callNextFilter(request);
  }
}
