package com.ft.up.apipolicy.pipeline;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import com.ft.up.apipolicy.configuration.Policy;

/**
 * MutableRequest
 *
 * @author Simon.Gibbs
 */
public class MutableRequest {

    private MultivaluedMap<String, String> headers;
    private MultivaluedMap<String, String> queryParameters;
    private String absolutePath;
    private String httpMethod;
    private byte[] requestEntity;

    private final Set<Policy> policies;
    private String transactionId;

    public MutableRequest(final Set<Policy> policies, final String transactionId) {
        this.policies = Collections.unmodifiableSet(policies);
        this.transactionId = transactionId;
    }

    public Set<Policy> getPolicies() {
        return policies;
    }

    public boolean policyIs(final Policy policy) {
        return policies.contains(policy);
    }

    public MultivaluedMap<String,String> getHeaders() {
        return headers;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequestEntityAsString() {
        return new String(requestEntity);
    }

    public void setRequestEntity(byte[] requestEntity) {
        this.requestEntity = requestEntity;
    }

    public void setQueryParameters(MultivaluedMap<String, String> queryParameters) {
        this.queryParameters = queryParameters;
    }
    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
