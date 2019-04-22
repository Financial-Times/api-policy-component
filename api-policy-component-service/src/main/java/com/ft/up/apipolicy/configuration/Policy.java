package com.ft.up.apipolicy.configuration;

public enum Policy {
    INCLUDE_RICH_CONTENT("INCLUDE_RICH_CONTENT"),
    INCLUDE_IDENTIFIERS("INCLUDE_IDENTIFIERS"),
    INCLUDE_COMMENTS("INCLUDE_COMMENTS"),
    INCLUDE_PROVENANCE("INCLUDE_PROVENANCE"),
    INCLUDE_LAST_MODIFIED_DATE("INCLUDE_LAST_MODIFIED_DATE"),
    INTERNAL_UNSTABLE("INTERNAL_UNSTABLE"),
    EXPAND_RICH_CONTENT("EXPAND_RICH_CONTENT"),
    RESTRICT_NON_SYNDICATABLE_CONTENT("RESTRICT_NON_SYNDICATABLE_CONTENT"),
    INTERNAL_ANALYTICS("INTERNAL_ANALYTICS");

    private final String headerValue;

    Policy(final String headerValue) {
        this.headerValue = headerValue;
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
