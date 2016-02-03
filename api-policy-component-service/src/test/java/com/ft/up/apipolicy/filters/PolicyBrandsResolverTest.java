package com.ft.up.apipolicy.filters;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class PolicyBrandsResolverTest {

    public static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String ALPHAVILLE_BRAND = "http://api.ft.com/things/89d15f70-640d-11e4-9803-0800200c9a66";
    public static final String BEYONDBRICS_BRAND = "http://api.ft.com/things/3a37a89e-14ce-4ac8-af12-961a9630dce3";

    private List<PolicyFilterParameter> policyFilterParameterList;
    private PolicyBrandsResolver policyBrandsResolver;

    private Set<Policy> policySet;

    @Mock
    private MutableRequest mutableRequest;

    @Mock
    private MultivaluedMap<String, String> queryParameters;

    private PolicyFilterParameter onlyFastFT;
    private PolicyFilterParameter excludeFastFT;

    private List<String> fastFTBrandList = Arrays.asList(FASTFT_BRAND);

    @Before
    public void setUp() {
        policyFilterParameterList = new ArrayList<>();

        onlyFastFT = new PolicyFilterParameter(Policy.FASTFT_CONTENT_ONLY, fastFTBrandList, null);
        excludeFastFT = new PolicyFilterParameter(Policy.EXCLUDE_FASTFT_CONTENT, null, fastFTBrandList);

        policyFilterParameterList.add(0, onlyFastFT);
        policyFilterParameterList.add(1, excludeFastFT);

        policyBrandsResolver = new PolicyBrandsResolver(policyFilterParameterList);

        when(mutableRequest.getQueryParameters()).thenReturn(queryParameters);
    }

    @Test
    public void noParametersShouldBeAddedWhenNull(){
        policySet = new HashSet<>();
        policySet.add(Policy.INCLUDE_COMMENTS);
        when(mutableRequest.getPolicies()).thenReturn(policySet);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(mutableRequest, never()).getQueryParameters();
        verify(queryParameters, never()).add("","");
    }

    @Test
    public void brandParametersShouldBeAddedWhenPolicyIsFastFtContentOnly(){
        policySet = new HashSet<>();
        policySet.add(Policy.FASTFT_CONTENT_ONLY);
        ArgumentCaptor<String> keyArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueArgument = ArgumentCaptor.forClass(String.class);
        when(mutableRequest.getPolicies()).thenReturn(policySet);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(queryParameters).add(keyArgument.capture(), valueArgument.capture());
        verify(mutableRequest, times(1)).getQueryParameters();
        assertEquals("forBrand was not passed in to add method", "forBrand", keyArgument.getValue());
        assertEquals("FASTFT brand was not passed in to add method", FASTFT_BRAND, valueArgument.getValue());
    }

    @Test
    public void brandParametersShouldBeAddedWhenPolicyIsExcludeFastFtContent(){
        policySet = new HashSet<>();
        policySet.add(Policy.EXCLUDE_FASTFT_CONTENT);
        ArgumentCaptor<String> keyArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueArgument = ArgumentCaptor.forClass(String.class);
        when(mutableRequest.getPolicies()).thenReturn(policySet);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(queryParameters).add(keyArgument.capture(), valueArgument.capture());
        verify(mutableRequest, times(1)).getQueryParameters();
        assertEquals("notForBrand was not passed in to add method", "notForBrand", keyArgument.getValue());
        assertEquals("FASTFT brand was not passed in to add method", FASTFT_BRAND, valueArgument.getValue());
    }


}
