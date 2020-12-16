package com.ft.up.apipolicy.pipeline;

/**
 * RequestForwarder
 *
 * @author Simon.Gibbs
 */
public interface RequestForwarder {
  MutableResponse forwardRequest(MutableRequest request);
}
