package com.ft.up.apipolicy.filters;

import static javax.ws.rs.core.Response.Status.OK;

import com.ft.api.jaxrs.errors.ClientError;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Map;

public abstract class AccessFilterBase extends AbstractImageFilter {
  private static final String YES_VALUE = "yes";

  protected JsonConverter jsonConverter;
  protected Policy policy;
  protected String jsonProperty;

  public AccessFilterBase(JsonConverter jsonConverter, Policy policy, String jsonProperty) {
    this.jsonConverter = jsonConverter;
    this.policy = policy;
    this.jsonProperty = jsonProperty;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
    final MutableResponse originalResponse = chain.callNextFilter(request);

    if (OK.getStatusCode() != originalResponse.getStatus()
        || !jsonConverter.isJson(originalResponse)) {
      return originalResponse;
    }

    if (shouldSkipFilter(request)) {
      return originalResponse;
    }

    final Map<String, Object> content = jsonConverter.readEntity(originalResponse);

    FieldModifier modifier =
        (propertyKey, contentModel) -> {
          if (contentModel.containsKey(propertyKey)) {
            String property = (String) contentModel.get(propertyKey);
            if (YES_VALUE.equals(property)) {
              if (shouldRemoveProperty()) {
                contentModel.remove(propertyKey);
              }
              jsonConverter.replaceEntity(originalResponse, content);
            } else {
              throw ClientError.status(403).error("Access denied.").exception();
            }
          } else {
            if (shouldThrowClientError()) {
              throw ClientError.status(403).error("Access denied.").exception();
            }
          }
        };
    applyFilter(jsonProperty, modifier, content);
    jsonConverter.replaceEntity(originalResponse, content);
    return originalResponse;
  }

  protected abstract boolean shouldThrowClientError();

  protected abstract boolean shouldRemoveProperty();

  protected abstract boolean shouldSkipFilter(MutableRequest request);
}
