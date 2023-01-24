package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.util.FluentLoggingBuilder.MESSAGE;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.slf4j.MDC.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.util.FluentLoggingBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PolicyBasedJsonFilter implements ApiFilter {

  private static final String CLASS_NAME = PolicyBasedJsonFilter.class.toString();

  private static Pattern jsonPathToRegex(String jsonPath) {
    String regex =
        jsonPath
            .replaceAll("\\.", Matcher.quoteReplacement("\\."))
            .replaceAll("\\$", "^" + Matcher.quoteReplacement("\\$"))
            .replaceAll("\\[\\*\\]", Matcher.quoteReplacement("\\[\\d+\\]"))
            .replaceAll(
                "\\[(\\d+)\\]",
                Matcher.quoteReplacement("\\[") + "$1" + Matcher.quoteReplacement("\\]"))
            .replaceAll("\\*(.+)", Matcher.quoteReplacement("[^.]*") + "$1")
            .replaceAll("\\*$", Matcher.quoteReplacement(".*"));

    return Pattern.compile(regex);
  }

  private final Map<Pattern, List<String>> policyFilters;
  private final JsonConverter jsonConverter;

  /**
   * Constructor.
   *
   * @param filters a map of JSONPath to required policy. A mapping to a null policy indicates that
   *     the path is returned in all requests (whitelisted).
   */
  public PolicyBasedJsonFilter(Map<String, List<Policy>> filters) {
    this.policyFilters = new HashMap<>();
    filters.forEach(
        (k, v) ->
            this.policyFilters.put(
                jsonPathToRegex(k),
                Optional.ofNullable(v)
                    .map(f -> f.stream().map(Enum::toString).collect(Collectors.toList()))
                    .orElse(Collections.emptyList())));
    this.jsonConverter = new JsonConverter(new ObjectMapper());
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    final MutableResponse response = chain.callNextFilter(request);
    int status = response.getStatus();
    if (isSuccess(status) && (status != SC_NO_CONTENT)) {
      final Map<String, Object> content = jsonConverter.readEntity(response);
      allowValue(content, "$", request.getPolicies());
      jsonConverter.replaceEntity(response, content);
    }

    return response;
  }

  private boolean isSuccess(int httpStatus) {
    return (httpStatus / 100) == 2;
  }

  private boolean isAllowedPath(String path, Set<String> requestPolicies) {
    Optional<Pattern> pathKey =
        policyFilters.keySet().stream()
            .filter(pattern -> pattern.matcher(path).matches())
            .findAny();

    if (pathKey.isPresent()) {
      List<String> pathPolicies = policyFilters.get(pathKey.get());
      if (pathPolicies.isEmpty()) {
        // There's no policy requirement for the path.
        return true;
      }

      for (String pathPolicy : pathPolicies) {
        if (requestPolicies.contains(pathPolicy)) {
          return true;
        }
      }
    }

    FluentLoggingBuilder.getNewInstance(CLASS_NAME, "isAllowedPath")
        .withTransactionId(get("transaction_id"))
        .withField(
            MESSAGE, path + " is not allowed by any policies among " + requestPolicies.toString())
        .build()
        .logDebug();

    return false;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean allowValue(Object value, String ctx, Set<String> policies) {
    boolean allow = true;

    if (value instanceof Map) {
      processObject((Map) value, ctx, policies);
    } else if (value instanceof Collection) {
      processArray((Collection) value, ctx, policies);
    } else {
      allow = isAllowedPath(ctx, policies);
    }

    return allow;
  }

  private void processObject(Map<String, Object> object, String path, Set<String> policies) {
    for (Iterator<Map.Entry<String, Object>> it = object.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, Object> e = it.next();
      String ctx = path + "." + e.getKey();
      if (!allowValue(e.getValue(), ctx, policies)) {
        it.remove();
      }
    }
  }

  private void processArray(Collection<Object> array, String path, Set<String> policies) {
    int i = 0;

    for (Iterator<Object> it = array.iterator(); it.hasNext(); ) {
      Object e = it.next();
      String ctx = path + "[" + i + "]";
      if (!allowValue(e, ctx, policies)) {
        it.remove();
      }
      i++;
    }
  }
}
