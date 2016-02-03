package com.ft.up.apipolicy.configuration;

import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.sun.jersey.api.client.WebResource;

public enum Policy {

    FASTFT_CONTENT_ONLY("FASTFT_CONTENT_ONLY", false),
    EXCLUDE_FASTFT_CONTENT("EXCLUDE_FASTFT_CONTENT", false),
    ALPHAVILLE_CONTENT_ONLY("ALPHAVILLE_CONTENT_ONLY", false),
    EXCLUDE_ALPHAVILLE_CONTENT("EXCLUDE_ALPHAVILLE_CONTENT", false),
    INCLUDE_RICH_CONTENT("INCLUDE_RICH_CONTENT", false),
    INCLUDE_IDENTIFIERS("INCLUDE_IDENTIFIERS", false),
    INCLUDE_COMMENTS("INCLUDE_COMMENTS", false),
    INCLUDE_PROVENANCE("INCLUDE_PROVENANCE", false),
    INCLUDE_LAST_MODIFIED_DATE("INCLUDE_LAST_MODIFIED_DATE", false),
    INTERNAL_UNSTABLE("INTERNAL_UNSTABLE", true);

    private final String headerValue;
    private final boolean forwardable;
    
    private Policy(final String headerValue, boolean forwardable) {
        this.headerValue = headerValue;
        this.forwardable = forwardable;
    }

    public String getHeaderValue() {
        return headerValue;
    }
    
    public boolean isForwardable() {
      return forwardable;
    }
    
    public void applyTo(WebResource.Builder request) {
      if (forwardable) {
        request.header(HttpPipeline.POLICY_HEADER_NAME, headerValue);
      }
    }
}
