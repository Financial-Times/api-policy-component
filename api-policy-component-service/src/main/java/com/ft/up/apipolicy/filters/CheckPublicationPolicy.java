package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class CheckPublicationPolicy implements ApiFilter {
  private static final String POLICY = "X-Policy";
  private static final String PUBLICATION = "publication";
  private static final String PUBLICATION_PREFIX = "PBLC_READ_";
  private static final String PINK_FT = "88fdde6c-2aa4-4f78-af02-9f680097cfd6";
  private static final String THING = "http://www.ft.com/thing/";
  private final JsonConverter jsonConverter;

  public CheckPublicationPolicy(JsonConverter jsonConverter) {
    this.jsonConverter = jsonConverter;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    final MutableResponse response = chain.callNextFilter(request);
    if (isEligibleForPublicationPolicyCheck(response)) {
      final Map<String, Object> content = extractContent(response);
      MultivaluedMap<String, Object> httpHeaders = response.getHeaders();
      Set<String> policies = getPublicationPolicies(httpHeaders.get(POLICY));
      List<String> publication = convertObjectToStringList(content.get(PUBLICATION));
      if (!checkAccess(policies, publication)) {
        response.setStatus(403);
      }
    }
    return response;
  }

  private static Boolean checkAccess(Set<String> policies, List<String> publication) {
    for (String p : policies) {
      if (publication.contains(p)) {
        return true;
      }
    }
    // No publication related X-Policy or publication field we consider this legacy ft request
    return policies.isEmpty() && publication.contains(PINK_FT) || publication.isEmpty();
  }

  private static List<String> convertObjectToStringList(Object obj) {
    if (obj instanceof Collection) {
      List<String> p = new ObjectMapper().convertValue(obj, new TypeReference<List<String>>() {});
      return p.stream().map(var -> var.replaceFirst(THING, "")).collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  private Set<String> getPublicationPolicies(List<Object> headerPolicies) {
    Set<String> policies = new LinkedHashSet<>();
    if (headerPolicies != null) {
      headerPolicies.forEach(
          p -> {
            String x = (String) p;
            List<String> pblcs = new LinkedList<>(Arrays.asList(x.split("[ ,]")));
            pblcs.removeIf(n -> !n.contains(PUBLICATION_PREFIX));
            policies.addAll(
                pblcs.stream()
                    .map(var -> var.replaceFirst(PUBLICATION_PREFIX, ""))
                    .collect(Collectors.toList()));
          });
    }
    return policies;
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
