package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;

public class CheckPublicationPolicy implements ApiFilter {
  private static final String POLICY = "X-Policy";
  private static final String PUBLICATION = "publication";
  private static final String PUBLICATION_PREFIX = "PBLC_READ_";
  private static final String PINK_FT = "88fdde6c-2aa4-4f78-af02-9f680097cfd6";
  private static final String THING = "http://www.ft.com/thing/";
  private static final String ERROR_FIELD = "error";
  private static final String ERROR_MESSAGE = "access denied";
  private final JsonConverter jsonConverter;

  public CheckPublicationPolicy(JsonConverter jsonConverter) {
    this.jsonConverter = jsonConverter;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    final MutableResponse response = chain.callNextFilter(request);
    if (isEligibleForPublicationPolicyCheck(response)) {
      final Map<String, Object> content = extractContent(response);
      List<String> policies = new ArrayList<>(request.getPolicies());
      List<String> pubPolicies = getPublicationPolicies(policies);
      List<String> publication = convertObjectToStringList(content.get(PUBLICATION));
      if (!checkAccess(pubPolicies, publication)) {
        return createResponseWithErrorMessage(response, content);
      }
    }
    return response;
  }

  private MutableResponse createResponseWithErrorMessage(
      final MutableResponse response, final Map<String, Object> content) {
    response.setStatus(403);
    content.clear();
    content.put(ERROR_FIELD, ERROR_MESSAGE);
    jsonConverter.replaceEntity(response, content);
    return response;
  }

  private static Boolean checkAccess(List<String> policies, List<String> publication) {
    if (policies.stream().anyMatch(publication::contains)) {
      return true;
    }
    // No publication related X-Policy or publication field we consider this legacy ft request
    return policies.isEmpty() && (publication.contains(PINK_FT) || publication.isEmpty());
  }

  private static List<String> convertObjectToStringList(Object obj) {
    if (obj instanceof Collection) {
      List<String> p = new ObjectMapper().convertValue(obj, new TypeReference<List<String>>() {});
      return p.stream().map(var -> var.replaceFirst(THING, "")).collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  private List<String> getPublicationPolicies(List<String> policies) {
    return policies.stream()
        .map(p -> p.replaceAll("\\s", ""))
        .filter(p -> p.contains(PUBLICATION_PREFIX))
        .map(p -> p.replaceFirst(PUBLICATION_PREFIX, ""))
        .collect(Collectors.toList());
  }

  private boolean isEligibleForPublicationPolicyCheck(final MutableResponse response) {
    return !isNotOKResponse(response) && !isNotJson(response);
  }

  private boolean isNotOKResponse(final MutableResponse response) {
    return Response.Status.OK.getStatusCode() != response.getStatus();
  }

  private boolean isNotJson(final MutableResponse response) {
    return !jsonConverter.isJson(response);
  }

  private Map<String, Object> extractContent(final MutableResponse response) {
    return jsonConverter.readEntity(response);
  }
}
