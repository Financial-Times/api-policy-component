package com.ft.up.apipolicy.filters;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.up.apipolicy.configuration.Policy;

public class PolicyFilterParameter {

    private Policy policy;
    private List<String> forBrand;
    private List<String> notForBrand;



    public PolicyFilterParameter(@JsonProperty("policy") Policy policy,
                                 @JsonProperty("forBrand") List<String> forBrand,
                                 @JsonProperty("notForBrand") List<String> notForBrand) {
        this.policy = policy;
        this.forBrand = forBrand;
        this.notForBrand = notForBrand;
    }

    @NotNull
    public Policy getPolicy() {
        return policy;
    }

    public List<String> getForBrand() {
        return forBrand;
    }

    public List<String> getNotForBrand() {
        return notForBrand;
    }
}
