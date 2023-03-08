package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import java.util.Map;
import java.util.Set;

public class RemoveJsonPropertiesUnlessPolicyPresentFilter extends SuppressJsonPropertiesFilter {

  private final Set<Policy> policies;

  public RemoveJsonPropertiesUnlessPolicyPresentFilter(
      JsonConverter jsonConverter, Set<Policy> policies, String... jsonProperties) {
    super(jsonConverter, jsonProperties);
    this.policies = policies;
  }

  @Override
  protected boolean shouldPropertyFilteredOut(
      final String jsonProperty, final MutableRequest request, Map content) {
    return policies.stream().noneMatch(request::policyIs)
        && super.shouldPropertyFilteredOut(jsonProperty, request, content);
  }
}
