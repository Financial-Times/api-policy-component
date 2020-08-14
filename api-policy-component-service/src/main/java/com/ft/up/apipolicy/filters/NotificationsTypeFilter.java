package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;

public class NotificationsTypeFilter implements ApiFilter {

  private static final String REQUEST_URL_KEY = "requestUrl";
  private static final String LINKS_KEY = "links";
  private static final String HREF_KEY = "href";
  private static final String TYPE_KEY = "type";

  private JsonConverter converter;
  private Policy policy;

  public NotificationsTypeFilter(JsonConverter converter, Policy policy) {
    this.converter = converter;
    this.policy = policy;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    addQueryParams(request);

    MutableResponse response = chain.callNextFilter(request);

    if (response.getStatus() != 200) {
      return response;
    }
    Map<String, Object> content = converter.readEntity(response);
    if (!typeCheckSucceeds(content)) {
      throw new FilterException(
          new IllegalStateException("Notifications json response is not in expected format."));
    }

    stripTypeParam(content, REQUEST_URL_KEY);
    List links = (List) content.get(LINKS_KEY);
    if (links.isEmpty()) {
      converter.replaceEntity(response, content);
      return response;
    }
    stripTypeParam((Map) links.get(0), HREF_KEY);
    converter.replaceEntity(response, content);

    return response;
  }

  private void addQueryParams(MutableRequest request) {

    if (request.policyIs(policy)) {
      request.getQueryParameters().add(TYPE_KEY, "all");
    } else {
      List<String> typeParams = new ArrayList<>();
      typeParams.add("article");
      request.getQueryParameters().put(TYPE_KEY, typeParams);
    }
  }

  private void stripTypeParam(Map<String, Object> content, String key) {
    UriBuilder uriBuilder = UriBuilder.fromUri((String) content.get(key));
    uriBuilder.replaceQueryParam(TYPE_KEY, null);
    content.put(key, uriBuilder.build());
  }

  private boolean typeCheckSucceeds(Map<String, Object> content) {
    return content.get(REQUEST_URL_KEY) instanceof String
        && content.get(LINKS_KEY) instanceof List
        && linksArrayTypeCheckSucceeds(((List) content.get(LINKS_KEY)));
  }

  private boolean linksArrayTypeCheckSucceeds(List links) {
    return links.isEmpty()
        || links.get(0) instanceof Map && ((Map) links.get(0)).get(HREF_KEY) instanceof String;
  }
}
