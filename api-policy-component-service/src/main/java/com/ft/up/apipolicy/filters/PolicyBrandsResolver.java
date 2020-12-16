package com.ft.up.apipolicy.filters;

import static com.ft.up.apipolicy.util.FluentLoggingBuilder.MESSAGE;
import static org.slf4j.MDC.get;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.util.FluentLoggingBuilder;
import java.util.*;

public class PolicyBrandsResolver {

  private static final String CLASS_NAME = PolicyBrandsResolver.class.toString();
  private Map<String, PolicyFilterParameter> policyFilterParameterMap;

  public PolicyBrandsResolver(
      @JsonProperty("policyFilterParameterList")
          List<PolicyFilterParameter> policyFilterParameterList) {

    policyFilterParameterMap = new HashMap<>();

    for (PolicyFilterParameter policyFilterParameter : policyFilterParameterList) {
      policyFilterParameterMap.put(policyFilterParameter.getPolicy(), policyFilterParameter);
    }
  }

  public Map<String, PolicyFilterParameter> getPolicyFilterParameterMap() {
    return policyFilterParameterMap;
  }

  public void applyQueryParams(MutableRequest request) {

    Set<String> policySet = request.getPolicies();

    Set<String> notKnownPolicies = new HashSet<String>();
    for (String policy : policySet) {
      PolicyFilterParameter policyFilterParameter = policyFilterParameterMap.get(policy);
      if (policyFilterParameter == null) {
        notKnownPolicies.add(policy);
        continue;
      }
      if (policyFilterParameter.getForBrand() != null) {
        List<String> forBrands = policyFilterParameter.getForBrand();
        for (String brand : forBrands) {
          request.getQueryParameters().add("forBrand", brand);
        }
      }
      if (policyFilterParameter.getNotForBrand() != null) {
        List<String> notForBrands = policyFilterParameter.getNotForBrand();
        for (String brand : notForBrands) {
          request.getQueryParameters().add("notForBrand", brand);
        }
      }
    }
    if (!notKnownPolicies.isEmpty()) {
      FluentLoggingBuilder.getNewInstance(CLASS_NAME, "applyQueryParams")
          .withTransactionId(get("transaction_id"))
          .withField(
              MESSAGE,
              "A policy in the request did not map to a known policy in the system : "
                  + notKnownPolicies.toString())
          .build()
          .logWarn();
    }
  }
}
