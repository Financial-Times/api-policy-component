package com.ft.up.apipolicy.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyBrandsResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyBrandsResolver.class);
    private Map<Policy, PolicyFilterParameter> policyFilterParameterMap;

    public PolicyBrandsResolver(@JsonProperty("policyFilterParameterList") List<PolicyFilterParameter> policyFilterParameterList) {

        policyFilterParameterMap = new HashMap<>();

        for(PolicyFilterParameter policyFilterParameter : policyFilterParameterList){
            policyFilterParameterMap.put(policyFilterParameter.getPolicy(), policyFilterParameter);
        }
    }

    public Map<Policy, PolicyFilterParameter> getPolicyFilterParameterMap() {
        return policyFilterParameterMap;
    }


    public void applyQueryParams(MutableRequest request) {

        Set<Policy> policySet = request.getPolicies();

        for(Policy policy:policySet){
            PolicyFilterParameter policyFilterParameter = policyFilterParameterMap.get(policy);

            if(policyFilterParameter == null){
                LOGGER.warn("A policy in the request did not map to a known policy in the system : " + policy);
                continue;
            }
            if(policyFilterParameter.getForBrand() != null){
               List<String> forBrands = policyFilterParameter.getForBrand();
               for(String brand : forBrands){
                   request.getQueryParameters().add("forBrand", brand);
               }
            }
            if(policyFilterParameter.getNotForBrand() != null){
                List<String> notForBrands = policyFilterParameter.getNotForBrand();
                for(String brand : notForBrands){
                    request.getQueryParameters().add("notForBrand", brand);
                }
            }

        }


    }
}
