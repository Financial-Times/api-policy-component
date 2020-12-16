package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;

public class CanBeDistributedAccessFilter extends AccessFilterBase {

  private static final String CAN_BE_DISTRIBUTED_KEY = "canBeDistributed";

  public CanBeDistributedAccessFilter(JsonConverter jsonConverter, Policy policy) {
    super(jsonConverter, policy, CAN_BE_DISTRIBUTED_KEY);
  }

  @Override
  protected boolean shouldThrowClientError() {
    return false;
  }

  @Override
  protected boolean shouldRemoveProperty() {
    return true;
  }

  @Override
  protected boolean shouldSkipFilter(MutableRequest request) {
    return request.policyIs(policy);
  }
}
