package com.ft.up.apipolicy.pipeline;

/**
 * HttpPipelineChain
 *
 * @author Simon.Gibbs
 */
public class HttpPipelineChain {

  private final HttpPipeline pipeline;
  private int pointer = 0;

  public HttpPipelineChain(final HttpPipeline pipeline) {
    this.pipeline = pipeline;
  }

  public MutableResponse callNextFilter(final MutableRequest request) {

    ApiFilter nextFilter = pipeline.getFilter(pointer++);

    if (nextFilter == null) {
      return pipeline.forwardRequest(request);
    } else {
      return nextFilter.processRequest(request, this);
    }
  }
}
