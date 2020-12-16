package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;

public class PolicyFilterParameter {

  private String policy;
  private List<String> forBrand;
  private List<String> notForBrand;

  public PolicyFilterParameter(
      @JsonProperty("policy") String policy,
      @JsonProperty("forBrand") List<String> forBrand,
      @JsonProperty("notForBrand") List<String> notForBrand) {
    this.policy = policy;
    this.forBrand = forBrand;
    this.notForBrand = notForBrand;
  }

  @NotNull
  public String getPolicy() {
    return policy;
  }

  public List<String> getForBrand() {
    return forBrand;
  }

  public List<String> getNotForBrand() {
    return notForBrand;
  }
}
