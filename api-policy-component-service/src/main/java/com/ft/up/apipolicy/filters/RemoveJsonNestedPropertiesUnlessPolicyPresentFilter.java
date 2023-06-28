package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import java.util.Map;
import java.util.Set;

public class RemoveJsonNestedPropertiesUnlessPolicyPresentFilter
    extends SuppressJsonPropertiesFilter {

  private final Set<Policy> policies;

  public RemoveJsonNestedPropertiesUnlessPolicyPresentFilter(
      final JsonConverter jsonConverter, Set<Policy> policies, final String... jsonProperties) {
    super(jsonConverter, jsonProperties);
    this.policies = policies;
  }

  @Override
  protected boolean shouldPropertyFilteredOut(
      final String jsonProperty, final MutableRequest request, Map content) {
    return policies.stream().noneMatch(request::policyIs);
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
