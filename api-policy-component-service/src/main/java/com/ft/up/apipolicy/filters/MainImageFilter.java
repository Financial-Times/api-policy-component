package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.HashMap;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_IMAGES;

public class MainImageFilter implements ApiFilter {

    public static final String MAIN_IMAGE = "mainImage";

    private JsonConverter jsonConverter;

    public MainImageFilter(final JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final HashMap<String, Object> content = jsonConverter.readEntity(response);
        if (request.policyIs(INCLUDE_IMAGES)) {
            content.remove(MAIN_IMAGE);
            jsonConverter.replaceEntity(response, content);
        }
        return response;
    }
}
