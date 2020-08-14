package com.ft.up.apipolicy.pipeline;

/**
 * HttpPipeline
 *
 * @author Simon.Gibbs
 */
public class HttpPipeline {

  public static final String POLICY_HEADER_NAME = "X-Policy";

  private RequestForwarder forwarder;
  private ApiFilter[] filters;

  public HttpPipeline(final RequestForwarder forwarder, final ApiFilter... filters) {
    this.forwarder = forwarder;
    this.filters = filters;
  }

  public ApiFilter getFilter(final int pointer) {
    if (pointer >= filters.length) {
      return null;
    }
    return filters[pointer];
  }

  public MutableResponse forwardRequest(final MutableRequest request) {
    return forwarder.forwardRequest(request);
  }
}
