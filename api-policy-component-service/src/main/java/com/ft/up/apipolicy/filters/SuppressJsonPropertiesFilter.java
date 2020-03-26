package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class SuppressJsonPropertiesFilter extends AbstractImageFilter {

    private final JsonConverter jsonConverter;
    private final List<String> jsonProperties;

    public SuppressJsonPropertiesFilter(final JsonConverter jsonConverter, final String... jsonProperties) {
        this.jsonConverter = jsonConverter;
        this.jsonProperties = Arrays.asList(jsonProperties);
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        for (String jsonProperty : jsonProperties) {
            processResponse(request, response, content, jsonProperty);
        }
        return response;
    }

    private MutableResponse processResponse(MutableRequest request, MutableResponse response, Map<String, Object> content, String jsonProperty) {
        if (isListResponse(content)) {
            List<Map<String, Object>> modifiedListContent = new LinkedList<>();
            content.forEach((k, v) -> processJsonArray(request, (Map<String, Object>) v, jsonProperty, modifiedListContent));
            jsonConverter.replaceEntity(response, modifiedListContent);
            return response;
        }
        processJsonObject(request, response, content, jsonProperty);
        return response;
    }

    private boolean isListResponse(Map<String, Object> content) {
        return content.keySet().stream().allMatch(StringUtils::isNumeric);
    }

    private void processJsonArray(MutableRequest request, Map<String, Object> content, String jsonProperty, List<Map<String, Object>> modifiedListContent) {
        FieldModifier modifier = (jsonProp, contentModel) -> {
            if (shouldPropertyFilteredOut(jsonProp, request, contentModel)) {
                contentModel.remove(jsonProp);
            }
            modifiedListContent.add(contentModel);
        };

        applyFilter(jsonProperty, modifier, content);
    }

    private void processJsonObject(MutableRequest request, MutableResponse response, Map<String, Object> content, String jsonProperty) {
        FieldModifier modifier = (jsonProp, contentModel) -> {
            if (shouldPropertyFilteredOut(jsonProp, request, contentModel)) {
                contentModel.remove(jsonProp);
                jsonConverter.replaceEntity(response, content);
            }
        };

        applyFilter(jsonProperty, modifier, content);
    }

    protected boolean shouldPropertyFilteredOut(final String jsonProperty, MutableRequest request, final Map content) {
        return content.containsKey(jsonProperty);
    }
}
