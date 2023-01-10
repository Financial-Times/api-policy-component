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
  private static final String MONITOR_KEY = "monitor";
  private static final String ALL_CONTENT_TYPES = "All";
  private static final String ARTICLE_CONTENT_TYPE = "Article";
  private static final String LIVE_BLOG_POST_CONTENT_TYPE = "LiveBlogPost";
  private static final String LIVE_BLOG_PACKAGE_CONTENT_TYPE = "LiveBlogPackage";

  private final JsonConverter converter;
  private final Policy allTypesPolicy;
  private final Policy specificTypesPolicy;

  public NotificationsTypeFilter(
      JsonConverter converter, Policy allTypesPolicy, Policy specificTypesPolicy) {
    this.converter = converter;
    this.allTypesPolicy = allTypesPolicy;
    this.specificTypesPolicy = specificTypesPolicy;
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

    stripInternalParams(content, REQUEST_URL_KEY);

    List links = (List) content.get(LINKS_KEY);
    if (links.isEmpty()) {
      converter.replaceEntity(response, content);
      return response;
    }

    stripInternalParams((Map) links.get(0), HREF_KEY);

    converter.replaceEntity(response, content);

    return response;
  }

  private void addQueryParams(MutableRequest request) {
    List<String> typeParams = new ArrayList<>();
    List<String> monitorParams = new ArrayList<>();

    if (request.policyIs(allTypesPolicy)) {
      typeParams.add(ALL_CONTENT_TYPES);
      monitorParams.add(Boolean.TRUE.toString());
    } else if (request.policyIs(specificTypesPolicy)) {
      typeParams.add(ARTICLE_CONTENT_TYPE);
      typeParams.add(LIVE_BLOG_PACKAGE_CONTENT_TYPE);
      typeParams.add(LIVE_BLOG_POST_CONTENT_TYPE);
      monitorParams.add(Boolean.FALSE.toString());
    } else {
      typeParams.add(ARTICLE_CONTENT_TYPE);
      monitorParams.add(Boolean.FALSE.toString());
    }

    request.getQueryParameters().put(TYPE_KEY, typeParams);
    request.getQueryParameters().put(MONITOR_KEY, monitorParams);
  }

  private void stripInternalParams(Map<String, Object> content, String key) {
    UriBuilder uriBuilder = UriBuilder.fromUri((String) content.get(key));
    uriBuilder.replaceQueryParam(TYPE_KEY, null);
    uriBuilder.replaceQueryParam(MONITOR_KEY, null);
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
