package com.ft.up.apipolicy.resources;

import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.google.common.base.Preconditions;
import java.util.regex.Pattern;

public class KnownEndpoint implements Comparable<KnownEndpoint> {

  private final String uriRegex;
  private final HttpPipeline httpPipeline;
  private final Pattern uriPattern;

  public KnownEndpoint(String uriRegex, HttpPipeline httpPipeline) {
    Preconditions.checkNotNull(uriRegex, "uriRegex must not be null");
    Preconditions.checkNotNull(httpPipeline, "httpPipeline must not be null");

    this.uriRegex = uriRegex;
    this.httpPipeline = httpPipeline;

    this.uriPattern = Pattern.compile(uriRegex);
  }

  public String getUriRegex() {
    return uriRegex;
  }

  public HttpPipeline getPipeline() {
    return httpPipeline;
  }

  @Override
  public int compareTo(KnownEndpoint knownEndpoint) {
    return knownEndpoint.getUriRegex().compareTo(uriRegex);
  }

  public Pattern getUriPattern() {
    return uriPattern;
  }

  @Override
  public String toString() {
    return "KnownEndpoint: " + uriPattern;
  }
}
