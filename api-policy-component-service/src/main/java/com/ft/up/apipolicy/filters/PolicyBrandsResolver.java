package com.ft.up.apipolicy.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.up.apipolicy.pipeline.MutableRequest;

public class PolicyBrandsResolver {

    private Map<String, PolicyFilterParameter> policyFilterParameterMap;

    public PolicyBrandsResolver(@JsonProperty("policyFilterParameterList") List<PolicyFilterParameter> policyFilterParameterList) {

        policyFilterParameterMap = new HashMap<String, PolicyFilterParameter>();

        for(PolicyFilterParameter policyFilterParameter : policyFilterParameterList){
            policyFilterParameterMap.put(policyFilterParameter.getPolicy(), policyFilterParameter);
        }
    }

    public Map<String, PolicyFilterParameter> getPolicyFilterParameterMap() {
        return policyFilterParameterMap;
    }


    public void applyQueryParams(MutableRequest request) {

        Set<String> policySet = request.getPolicies();

        for(String policy:policySet){
            PolicyFilterParameter policyFilterParameter = policyFilterParameterMap.get(policy);

            if(policyFilterParameter == null){
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