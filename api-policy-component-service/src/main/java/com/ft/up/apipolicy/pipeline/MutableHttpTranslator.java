package com.ft.up.apipolicy.pipeline;

import static com.ft.api.util.transactionid.TransactionIdUtils.TRANSACTION_ID_HEADER;
import static com.ft.up.apipolicy.pipeline.HttpPipeline.POLICY_HEADER_NAME;
import static com.ft.up.apipolicy.util.FluentLoggingBuilder.MESSAGE;
import static com.ft.up.apipolicy.util.FluentLoggingBuilder.flattenHeaderToString;
import static java.util.Collections.emptySet;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.ft.api.jaxrs.errors.ServerError;
import com.ft.up.apipolicy.util.FluentLoggingBuilder;
import java.io.IOException;
import java.util.*;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MutableHttpTranslator
 *
 * @author Simon.Gibbs
 */
public class MutableHttpTranslator {

  private static final Logger LOGGER = LoggerFactory.getLogger(MutableHttpTranslator.class);
  private static final String CLASS_NAME = MutableHttpTranslator.class.toString();

  // So we don't blindly pass on headers that should be set by THIS application on the
  // request/response
  public Set<String> HEADER_BLACKLIST =
      new TreeSet<>(
          Arrays.asList(
              "Connection",
              "Accept-Encoding",
              "Content-Length",
              "Transfer-Encoding",
              "Content-Encoding",
              "Date",
              POLICY_HEADER_NAME));

  public MutableHttpTranslator() {}

  public MutableRequest translateFrom(HttpServletRequest realRequest) {
    FluentLoggingBuilder log =
        FluentLoggingBuilder.getNewInstance(CLASS_NAME, "translateFrom")
            .withRequest(realRequest)
            .withField(FluentLoggingBuilder.URI, realRequest.getRequestURI())
            .withField(FluentLoggingBuilder.PATH, realRequest.getContextPath());

    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    MultivaluedMap<String, String> blacklistedHeaders = new MultivaluedHashMap<>();
    Set<String> policies = emptySet();
    String transactionId = null;
    Enumeration<String> headerNames = realRequest.getHeaderNames();

    if (headerNames != null) {
      transactionId = realRequest.getHeader(TRANSACTION_ID_HEADER);
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        Enumeration<String> values = realRequest.getHeaders(headerName);
        List<String> headersValuesList = list(values);
        if (TRANSACTION_ID_HEADER.equals(headerName)) {
          continue;
        } else if (POLICY_HEADER_NAME.equalsIgnoreCase(headerName)) {
          policies = getPolicies(headersValuesList);
          headers.add(POLICY_HEADER_NAME, policies.toString());
        } else if (HEADER_BLACKLIST.contains(headerName)) {
          logBlacklistedHeader(headerName, headersValuesList, transactionId);
          blacklistedHeaders.addAll(headerName, headersValuesList);
        } else if (("Host").equals(headerName)) { // for Containerisation
          headers.add(headerName, "public-services");
        } else {
          headers.addAll(headerName.toLowerCase(), headersValuesList);
          logPassedHeaders(headerName, headersValuesList, "Passed Up: ", transactionId);
        }
      }

      // Always add the transaction ID including the default random one if it was missing
      headers.add(TRANSACTION_ID_HEADER, transactionId);
      log.withTransactionId(transactionId);

      if (!policies.isEmpty() && !isBlank(transactionId)) {
        log.withField(MESSAGE, "Processed " + POLICY_HEADER_NAME + " : " + policies)
            .build()
            .logDebug();
      } else {
        log.withField(MESSAGE, "No X-Policy Headers").build().logDebug();
      }
    } else {
      log.withField(MESSAGE, "No headers").build().logDebug();
    }

    return getMutableRequest(realRequest, headers, blacklistedHeaders, policies, transactionId);
  }

  private Set<String> getPolicies(List<String> values) {
    Set<String> policies = new LinkedHashSet<>();

    if (values != null) {
      values.forEach(value -> policies.addAll(Arrays.asList(value.split("[ ,]"))));
    }
    return policies;
  }

  private MutableRequest getMutableRequest(
      HttpServletRequest realRequest,
      MultivaluedMap<String, String> headers,
      MultivaluedMap<String, String> blacklistedHeaders,
      Set<String> policies,
      String transactionId) {
    MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
    Enumeration<String> parameterNames = realRequest.getParameterNames();
    if (parameterNames != null) {
      while (parameterNames.hasMoreElements()) {
        String queryParam = parameterNames.nextElement();
        queryParameters.put(
            queryParam, Arrays.asList(realRequest.getParameterMap().get(queryParam)));
      }
    }

    String absolutePath = java.net.URI.create(realRequest.getRequestURL().toString()).getPath();

    MutableRequest request = new MutableRequest(policies, transactionId);
    request.setAbsolutePath(absolutePath);
    request.setQueryParameters(queryParameters);
    request.setHeaders(headers);
    request.setBlacklistedHeaders(blacklistedHeaders);
    request.setRequestEntity(getEntityIfSupplied(realRequest));
    request.setHttpMethod(realRequest.getMethod());

    return request;
  }

  private void logBlacklistedHeader(String headerName, List<String> values, String transactionId) {
    if (LOGGER.isDebugEnabled() && !values.isEmpty()) {
      FluentLoggingBuilder.getNewInstance(CLASS_NAME, "logBlacklistedHeader")
          .withField(MESSAGE, "Not Processed: " + headerName + "=" + values)
          .withTransactionId(transactionId)
          .build()
          .logDebug();
    }
  }

  private void logPassedHeaders(
      String headerName, List<String> values, String msgParam, String transactionId) {
    FluentLoggingBuilder.getNewInstance(CLASS_NAME, "logPassedHeaders")
        .withField(MESSAGE, msgParam + headerName + "=" + values.toString())
        .withTransactionId(transactionId)
        .build()
        .logDebug();
  }

  private byte[] getEntityIfSupplied(HttpServletRequest realRequest) {
    try {
      byte[] entity = null;
      ServletInputStream inputStream = realRequest.getInputStream();
      if (inputStream != null) {
        entity = IOUtils.toByteArray(inputStream);
      }
      return entity;
    } catch (IOException e) {
      throw ServerError.status(500).error(e.getMessage()).exception(e);
    }
  }

  public ResponseBuilder translateTo(MutableResponse mutableResponse) {

    String tid = flattenHeaderToString(mutableResponse, TRANSACTION_ID_HEADER);

    ResponseBuilder responseBuilder = status(mutableResponse.getStatus());

    for (String headerName : mutableResponse.getHeaders().keySet()) {

      List<Object> values = mutableResponse.getHeaders().get(headerName);

      List<String> valuesAsStrings =
          values.stream().map(object -> Objects.toString(object, null)).collect(toList());

      if (HEADER_BLACKLIST.contains(headerName)) {
        logBlacklistedHeader(headerName, valuesAsStrings, tid);
      } else {
        logPassedHeaders(headerName, valuesAsStrings, "Passed Down: ", tid);
        valuesAsStrings.forEach(value -> responseBuilder.header(headerName.toLowerCase(), value));
      }
    }
    responseBuilder.entity(mutableResponse.getEntity());

    return responseBuilder;
  }
}
