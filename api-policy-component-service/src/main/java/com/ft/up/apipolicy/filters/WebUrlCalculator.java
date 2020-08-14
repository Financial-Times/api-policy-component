package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response.Status;

public class WebUrlCalculator implements ApiFilter {
  private static final String BODY_KEY = "bodyXML";
  private static final String WEB_URL_KEY = "webUrl";
  private static final String TYPE_KEY = "type";
  private static final String TYPES_KEY = "types";
  private static final String IDENTIFIERS_KEY = "identifiers";
  private static final String AUTHORITY_KEY = "authority";
  private static final String NEXT_VIDEO_EDITOR_AUTHORITY =
      "http://api.ft.com/system/NEXT-VIDEO-EDITOR";

  private static final String ARTICLE_TYPE = "http://www.ft.com/ontology/content/Article";

  private final Map<String, String> urlTemplates;
  private JsonConverter jsonConverter;

  public WebUrlCalculator(final Map<String, String> urlTemplates, final JsonConverter converter) {
    this.urlTemplates = urlTemplates;
    this.jsonConverter = converter;
  }

  @Override
  public MutableResponse processRequest(
      final MutableRequest request, final HttpPipelineChain chain) {
    final MutableResponse originalResponse = chain.callNextFilter(request);
    if (isEligibleForWebUrl(originalResponse)) {
      final Map<String, Object> content = extractContent(originalResponse);
      if (!content.containsKey(WEB_URL_KEY)) {
        return createResponseWithWebUrlCompleted(originalResponse, content);
      }
    }
    return originalResponse;
  }

  private boolean isEligibleForWebUrl(final MutableResponse response) {
    if (isNotOKResponse(response) || isNotJson(response)) {
      return false;
    }
    final Map<String, Object> content = jsonConverter.readEntity(response);

    boolean bodyPresent = content.containsKey(BODY_KEY);

    return bodyPresent || isArticle(content) || isVideo(content);
  }

  private boolean isArticle(Map<String, Object> content) {
    Object type = content.get(TYPE_KEY);
    Object types = content.get(TYPES_KEY);
    return ARTICLE_TYPE.equals(type)
        || ((types instanceof Collection) && ((Collection) types).contains(ARTICLE_TYPE));
  }

  private boolean isVideo(Map<String, Object> content) {
    if (content.containsKey(IDENTIFIERS_KEY)) {
      final Object identifiersRaw = content.get(IDENTIFIERS_KEY);
      if (identifiersRaw instanceof Collection) {
        Iterator identifiersIterator = ((Collection) identifiersRaw).iterator();
        if (identifiersIterator.hasNext()) {
          Object identifierRaw = identifiersIterator.next();
          if (identifierRaw instanceof Map) {
            Map identifier = ((Map) identifierRaw);
            if (identifier.containsKey(AUTHORITY_KEY)) {
              final Object authority = identifier.get(AUTHORITY_KEY);
              if (NEXT_VIDEO_EDITOR_AUTHORITY.equals(authority)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  private boolean isNotOKResponse(final MutableResponse response) {
    return Status.OK.getStatusCode() != response.getStatus();
  }

  private boolean isNotJson(final MutableResponse response) {
    return !jsonConverter.isJson(response);
  }

  private Map<String, Object> extractContent(final MutableResponse response) {
    return jsonConverter.readEntity(response);
  }

  private MutableResponse createResponseWithWebUrlCompleted(
      final MutableResponse response, final Map<String, Object> content) {
    final String webUrl = generateWebUrlFromIdentifiers(content);
    if (webUrl != null) {
      content.put(WEB_URL_KEY, webUrl);
      jsonConverter.replaceEntity(response, content);
      return response;
    }
    return response;
  }

  private String generateWebUrlFromIdentifiers(Map<String, Object> content) {
    @SuppressWarnings("unchecked")
    List<Map<String, String>> identifiers = (List<Map<String, String>>) content.get("identifiers");
    if (identifiers != null) {
      for (Map<String, String> map : identifiers) {
        String authority = map.get("authority");
        String value = map.get("identifierValue");
        for (String key : urlTemplates.keySet()) {
          if (Pattern.matches(key, authority)) {

            String template = urlTemplates.get(key);
            if (template != null) {
              return template.replace("{{identifierValue}}", value);
            }
            break;
          }
        }
      }
    }
    return null;
  }
}
