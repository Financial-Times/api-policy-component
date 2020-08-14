package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import java.util.Map;

public class RemoveJsonPropertiesUnlessPolicyPresentFilter extends SuppressJsonPropertiesFilter {

  private final Policy policy;

  public RemoveJsonPropertiesUnlessPolicyPresentFilter(
      final JsonConverter jsonConverter, final Policy policy, final String... jsonProperties) {
    super(jsonConverter, jsonProperties);
    this.policy = policy;
  }

  @Override
  protected boolean shouldPropertyFilteredOut(
      final String jsonProperty, final MutableRequest request, Map content) {
    return !request.policyIs(policy)
        && super.shouldPropertyFilteredOut(jsonProperty, request, content);
  }
}
