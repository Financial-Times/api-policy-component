package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;

public class SuppressRichContentMarkupFilter implements ApiFilter {

  private static final Set<String> XML_KEYS = ImmutableSet.of("bodyXML", "openingXML");
  private final JsonConverter jsonConverter;
  private BodyProcessingFieldTransformer transformer;

  public SuppressRichContentMarkupFilter(
      JsonConverter jsonConverter, BodyProcessingFieldTransformer transformer) {
    this.jsonConverter = jsonConverter;
    this.transformer = transformer;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

    MutableResponse response = chain.callNextFilter(request);

    if (request.policyIs(INCLUDE_RICH_CONTENT)) {
      return response;
    }

    if (!jsonConverter.isJson(response)) {
      return response;
    }

    Map<String, Object> content = jsonConverter.readEntity(response);

    for (String key : XML_KEYS) {
      String xml = (String) content.get(key);
      if (!Strings.isNullOrEmpty(xml)) {
        xml = transformer.transform(xml, request.getTransactionId());
        content.put(key, xml);
      }
    }

    jsonConverter.replaceEntity(response, content);

    return response;
  }
}
