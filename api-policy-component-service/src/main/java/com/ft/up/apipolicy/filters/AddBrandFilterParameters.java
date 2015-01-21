package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;

import static com.ft.up.apipolicy.configuration.Policy.*;

/**
 * AddBrandFilterParameters
 *
 * @author Simon.Gibbs
 */
public class AddBrandFilterParameters implements ApiFilter {

    public static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String ALPHAVILLE_BRAND = "http://api.ft.com/things/89d15f70-640d-11e4-9803-0800200c9a66";
    public static final String REQUEST_URL_KEY = "requestUrl";
    private JsonConverter converter;

    public AddBrandFilterParameters(JsonConverter jsonConverter) {
        this.converter = jsonConverter;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

        if(request.policyIs(FASTFT_CONTENT_ONLY)) {
            request.getQueryParameters().add("forBrand",FASTFT_BRAND);
        }

        if(request.policyIs(EXCLUDE_FASTFT_CONTENT)) {
            request.getQueryParameters().add("notForBrand",FASTFT_BRAND);
        }
        
        if(request.policyIs(ALPHAVILLE_CONTENT_ONLY)) {
            request.getQueryParameters().add("forBrand",ALPHAVILLE_BRAND);
        }

        if(request.policyIs(EXCLUDE_ALPHAVILLE_CONTENT)) {
            request.getQueryParameters().add("notForBrand",ALPHAVILLE_BRAND);
        }

        MutableResponse response = chain.callNextFilter(request);

        if(response.getStatus()!=200) {
            return response;
        }

        HashMap<String, Object> content = converter.readEntity(response);

        UriBuilder requestUriBuilder = UriBuilder.fromUri((String)content.get(REQUEST_URL_KEY));
        requestUriBuilder.replaceQueryParam("notForBrand", null);
        requestUriBuilder.replaceQueryParam("forBrand",null);

        content.put(REQUEST_URL_KEY, requestUriBuilder.build());


        converter.replaceEntity(response, content);

        return response;

    }

}
