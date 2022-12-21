package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import java.util.Map;

public class RemoveJsonNestedPropertiesUnlessPolicyPresentFilter
    extends SuppressJsonPropertiesFilter {

  private final Policy policy;

  public RemoveJsonNestedPropertiesUnlessPolicyPresentFilter(
      final JsonConverter jsonConverter, final Policy policy, final String... jsonProperties) {
    super(jsonConverter, jsonProperties);
    this.policy = policy;
  }

  @Override
  protected boolean shouldPropertyFilteredOut(
      final String jsonProperty, final MutableRequest request, Map content) {
    return !request.policyIs(policy);
  }

  @Override
  protected void removeProperty(Map contentModel, String jsonProp) {
    Map temp = contentModel;
    String[] path = jsonProp.split("\\.");
    for (int i = 0; i < path.length - 1; i++) {
      Object step = temp.get(path[i]);
      if (!(step instanceof Map)) {
        // nested key invalid or not found
        return;
      }
      temp = (Map) step;
    }
    temp.remove(path[path.length - 1]);
  }
}
