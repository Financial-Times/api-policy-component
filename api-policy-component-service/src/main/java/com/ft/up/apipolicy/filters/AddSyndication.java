package com.ft.up.apipolicy.filters;

import static javax.ws.rs.core.Response.Status.OK;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Map;

public class AddSyndication extends AbstractImageFilter {

  private static final String CAN_BE_SYNDICATED_KEY = "canBeSyndicated";
  private static final String CAN_BE_SYNDICATED_VERIFY = "verify";

  private JsonConverter jsonConverter;

  public AddSyndication(final JsonConverter jsonConverter) {
    this.jsonConverter = jsonConverter;
  }

  @Override
  public MutableResponse processRequest(
      final MutableRequest request, final HttpPipelineChain chain) {
    final MutableResponse originalResponse = chain.callNextFilter(request);
    if (!isEligibleForSyndicationField(originalResponse)) {
      return originalResponse;
    }
    final Map<String, Object> content = jsonConverter.readEntity(originalResponse);
    FieldModifier modifier =
        (jsonProp, contentModel) -> {
          if (!contentModel.containsKey(jsonProp)) {
            contentModel.put(jsonProp, CAN_BE_SYNDICATED_VERIFY);
          }
        };

    applyFilter(CAN_BE_SYNDICATED_KEY, modifier, content);
    jsonConverter.replaceEntity(originalResponse, content);
    return originalResponse;
  }

  private boolean isEligibleForSyndicationField(final MutableResponse response) {
    return OK.getStatusCode() == response.getStatus() && jsonConverter.isJson(response);
  }
}
