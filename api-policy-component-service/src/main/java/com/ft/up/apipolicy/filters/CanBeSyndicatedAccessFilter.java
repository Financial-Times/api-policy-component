package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;

public class CanBeSyndicatedAccessFilter extends AccessFilterBase {

  private static final String CAN_BE_SYNDICATED_KEY = "canBeSyndicated";

  public CanBeSyndicatedAccessFilter(JsonConverter jsonConverter, Policy policy) {
    super(jsonConverter, policy, CAN_BE_SYNDICATED_KEY);
  }

  @Override
  protected boolean shouldThrowClientError() {
    return true;
  }

  @Override
  protected boolean shouldRemoveProperty() {
    return false;
  }

  @Override
  protected boolean shouldSkipFilter(MutableRequest request) {
    return !request.policyIs(policy);
  }
}
