package com.ft.up.apipolicy.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class PolicyBasedJsonFilter implements ApiFilter {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyBasedJsonFilter.class);
  
  private static Pattern jsonPathToRegex(String jsonPath) {
    String regex = jsonPath.replaceAll("\\.", Matcher.quoteReplacement("\\."))
        .replaceAll("\\$", "^" + Matcher.quoteReplacement("\\$"))
        .replaceAll("\\[\\*\\]", Matcher.quoteReplacement("\\[\\d+\\]"))
        .replaceAll("\\[(\\d+)\\]", Matcher.quoteReplacement("\\[") + "$1" + Matcher.quoteReplacement("\\]"))
        .replaceAll("\\*(.+)", Matcher.quoteReplacement("[^.]*") + "$1")
        .replaceAll("\\*$", Matcher.quoteReplacement(".*"));
    
    Pattern p = Pattern.compile(regex);
    return p;
  }
  
  private Map<Pattern,String> policyFilters = new HashMap<>();
  private JsonConverter jsonConverter = new JsonConverter(new ObjectMapper());
  
  /** Constructor.
   * @param filters a map of JSONPath to required policy. A mapping to a null policy indicates that the path is returned in all requests (whitelisted).
   */
  public PolicyBasedJsonFilter(Map<String,Policy> filters) {
    filters.forEach((k, v) -> policyFilters.put(jsonPathToRegex(k), (v == null) ? null : v.toString()));
  }
  
  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    final MutableResponse response = chain.callNextFilter(request);

    final Map<String, Object> content = jsonConverter.readEntity(response);
    allowValue(content, "$", request.getPolicies());
    jsonConverter.replaceEntity(response, content);
    
    return response;
  }
  
  private boolean isAllowedPath(String path, Set<String> policies) {
    for (Map.Entry<Pattern,String> en : policyFilters.entrySet()) {
      if (en.getKey().matcher(path).matches()) {
        String requiredPolicy = en.getValue();
        
        if ((requiredPolicy == null) || policies.contains(en.getValue())) {
          return true;
        }
      }
    }
    
    LOG.debug("{} is not allowed by any policies among {}", path, policies);
    return false;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean allowValue(Object value, String ctx, Set<String> policies) {
    boolean allow = true;
    
    if (value instanceof Map) {
      processObject((Map)value, ctx, policies);
    } else if (value instanceof Collection) {
      processArray((Collection)value, ctx, policies);
    } else {
      allow = isAllowedPath(ctx, policies);
    }
    
    return allow;
  }

  private void processObject(Map<String,Object> object, String path, Set<String> policies) {
    for (Iterator<Map.Entry<String,Object>> it = object.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String,Object> e = it.next();
      String ctx = path + "." + e.getKey();
      if (!allowValue(e.getValue(), ctx, policies)) {
        LOG.debug("remove {}", ctx);
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
        LOG.debug("remove {}", ctx);
        it.remove();
      }
      
      i++;
    };
  }
}
