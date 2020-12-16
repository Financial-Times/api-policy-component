package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.google.common.base.Strings;
import java.util.Map;

public class SuppressInternalContentFilter implements ApiFilter {

  private static final String BODY_XML_KEY = "bodyXML";
  private final JsonConverter jsonConverter;
  private BodyProcessingFieldTransformer transformer;

  public SuppressInternalContentFilter(
      JsonConverter jsonConverter, BodyProcessingFieldTransformer transformer) {
    this.jsonConverter = jsonConverter;
    this.transformer = transformer;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    MutableResponse response = chain.callNextFilter(request);

    if (!jsonConverter.isJson(response)) {
      return response;
    }

    Map<String, Object> content = jsonConverter.readEntity(response);
    String xml = (String) content.get(BODY_XML_KEY);
    if (!Strings.isNullOrEmpty(xml)) {
      xml = transformer.transform(xml, request.getTransactionId());
      content.put(BODY_XML_KEY, xml);
    }

    jsonConverter.replaceEntity(response, content);
    return response;
  }
}
