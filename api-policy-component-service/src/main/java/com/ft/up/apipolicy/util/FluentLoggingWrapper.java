package com.ft.up.apipolicy.util;

import com.ft.membership.logging.IntermediateYield;
import com.ft.membership.logging.Operation;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;

import static com.ft.api.util.transactionid.TransactionIdUtils.TRANSACTION_ID_HEADER;
import static com.ft.membership.logging.Operation.operation;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.commons.lang.StringUtils.*;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

public class FluentLoggingWrapper {

    private final static Logger logger = LoggerFactory.getLogger(FluentLoggingWrapper.class);

    public static final String SYSTEM_CODE = "systemcode";
    public static final String CONTENT_TYPE = "content-type";
    public static final String STATUS = "status";
    public static final String USER_AGENT = "userAgent";
    public static final String EXCEPTION = "exception_message";
    public static final String STACKTRACE = "stacktrace_log";
    public static final String CLIENT = "client";
    public static final String METHOD = "method";
    public static final String HOST = "host";
    public static final String ACCEPT = "accept";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String URI = "uri";
    public static final String PATH = "path";
    public static final String UUID = "uuid";
    public static final String MESSAGE = "msg";
    public static final String PROTOCOL = "protocol";
    public static final String APPLICATION_NAME = "api-policy-component";
    public static final String RUNBOOK_URI = "https://runbooks.in.ft.com/api-policy-component";


    private Map<String, Object> items;
    private String methodName;
    private String loggingClassName;


    public FluentLoggingWrapper() {
        items = new HashMap<>();
    }

    public FluentLoggingWrapper withField(final String key, final Object val) {
        if (isBlank(key) || isNull(val) || isBlank(valueOf(val))) {
            return this;
        }
        items.put(key, val);
        return this;
    }

    public FluentLoggingWrapper withMethodName(final String name) {
        methodName = name;
        return this;
    }

    public FluentLoggingWrapper withClassName(final String name) {
        loggingClassName = name;
        return this;
    }

    public FluentLoggingWrapper withException(Throwable t) {
        if (nonNull(t)) {
            withField(EXCEPTION, t.getMessage());

            if (logger.isDebugEnabled()) {
                withField(STACKTRACE, getStackTrace(t));
            }
        } else {
            withField(EXCEPTION, "Exception was null");
        }

        return this;
    }

    public FluentLoggingWrapper withInboundHeaders(HttpHeaders headers) {
        if (nonNull(headers)) {
            withField(ACCEPT, headers.getHeaderString(headers.ACCEPT));
            withField(USER_AGENT, headers.getHeaderString(headers.USER_AGENT));
        }
        return this;
    }

    public FluentLoggingWrapper withRequest(HttpServletRequest request) {
        if (nonNull(request)) {
            withField(PROTOCOL, request.getProtocol());
            withField(HOST, request.getLocalAddr());
            withField(METHOD, request.getMethod());
            withField(CLIENT, request.getRemoteAddr());
            withHttpServletRequestHeaders(request);
        }
        return this;
    }

    private void withHttpServletRequestHeaders(HttpServletRequest request) {
        withField(ACCEPT, request.getHeader("accept"));
        withField(CONTENT_TYPE, request.getHeader("content-type"));
        withField(USER_AGENT, request.getHeader("user-agent"));
    }

    public FluentLoggingWrapper withRequest(MutableRequest request) {
        if (nonNull(request)) {
            withField(METHOD, request.getHttpMethod());
            withHttpServletRequestHeaders(request);
        }
        return this;
    }

    private void withHttpServletRequestHeaders(MutableRequest request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        if (nonNull(headers)) {
            withField(USER_AGENT, headers.get("user-agent"));
            withField(ACCEPT, headers.get("accept"));
            withField(CONTENT_TYPE, headers.get("content-type"));
        }
    }

    public FluentLoggingWrapper withResponse(Response response) {
        if (nonNull(response)) {
            withField(STATUS, valueOf(response.getStatus()));
        }
        withOutboundHeaders(response);
        return this;
    }

    private FluentLoggingWrapper withOutboundHeaders(Response response) {
        String contentTypeHeader = flattenHeaderToString(response, CONTENT_TYPE);
        withField(CONTENT_TYPE, contentTypeHeader);
        withField(USER_AGENT, getOutboundUserAgentHeader());
        withField(ACCEPT, APPLICATION_JSON_TYPE.toString());
        return this;
    }

    public FluentLoggingWrapper withResponse(MutableResponse response) {
        if (nonNull(response)) {
            withField(STATUS, valueOf(response.getStatus()));
        }
        withOutboundHeaders(response);
        return this;
    }

    private FluentLoggingWrapper withOutboundHeaders(MutableResponse response) {
        String contentTypeHeader = flattenHeaderToString(response, CONTENT_TYPE);
        withField(CONTENT_TYPE, contentTypeHeader);
        withField(USER_AGENT, getOutboundUserAgentHeader());
        withField(ACCEPT, APPLICATION_JSON_TYPE.toString());
        return this;
    }

    public FluentLoggingWrapper withTransactionId(final String transactionId) {
        withField(TRANSACTION_ID_HEADER, transactionId);
        withField(TRANSACTION_ID, transactionId);
        return this;
    }

    public FluentLoggingWrapper withUriInfo(UriInfo ui) {
        if (nonNull(ui)) {
            withField(URI, ui.getAbsolutePath().toString());
            withField(PATH, ui.getPath());
        }
        return this;
    }

    public FluentLoggingWrapper withUri(java.net.URI uri) {
        if (nonNull(uri)) {
            withField(URI, uri.toString());
            withField(PATH, uri.getPath());
        }
        return this;
    }

    public IntermediateYield build() {
        Operation operationJson = operation(methodName)
                .jsonLayout()
                .initiate(loggingClassName);
        IntermediateYield iy = operationJson.logIntermediate();
        iy.yielding("class", loggingClassName);
        iy.yielding(SYSTEM_CODE, APPLICATION_NAME);
        iy.yielding(items);

        items = new HashMap<>();

        return iy;
    }

    private static String flattenHeaderToString(Response response, String headerKey) {
        if (isNull(response) || isNull(response.getHeaders())) {
            return EMPTY;
        }
        return ofNullable(response.getHeaders().get(headerKey))
                .map(headers -> headers.stream().map(Object::toString).collect(joining(";")))
                .orElse("");
    }

    public static String flattenHeaderToString(MutableResponse response, String headerKey) {
        if (isNull(response) || isNull(response.getHeaders())) {
            return EMPTY;
        }
        return ofNullable(response.getHeaders().get(headerKey))
                .map(headers -> headers.stream().map(Object::toString).collect(joining(";")))
                .orElse("");
    }

    private static String getOutboundUserAgentHeader() {
        String gitTag = System.getProperty("gitTag");
        String userAgentValue;
        if (isNotEmpty(gitTag)) {
            userAgentValue = APPLICATION_NAME + "/" + gitTag + " (+" + RUNBOOK_URI + ")";
        } else {
            userAgentValue = APPLICATION_NAME + " (+" + RUNBOOK_URI + ")";
        }
        return userAgentValue;
    }
}
