package com.ft.up.apipolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ft.up.apipolicy.filters.FilterException;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonConverter
 *
 * @author Simon.Gibbs
 */
public class JsonConverter {

    public static final TypeReference<LinkedHashMap<String, Object>> JSON_MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {
    };
    public static final TypeReference<LinkedHashMap<String, Object>[]> JSON_ARRAY_TYPE = new TypeReference<LinkedHashMap<String, Object>[]>() {
    };

    public static JsonConverter testConverter() {
        return new JsonConverter(new ObjectMapper());
    }

    private final ObjectMapper objectMapper;

    public JsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Map.class, new MutableResponseDeserializer());
        this.objectMapper.registerModule(module);
    }

    public boolean isJson(MutableResponse response) {
        if (response.getContentType() == null) {
            throw new NullPointerException("Content-Type not set");
        }
        return response.getContentType().startsWith(MediaType.APPLICATION_JSON);
    }

    public Map<String, Object> readEntity(MutableResponse response) {
        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(Map.class, String.class, Object.class);
            return objectMapper.readValue(response.getEntityAsString(), mapType);
        } catch (IOException e) {
            throw new FilterException(e);
        }
    }

    public void replaceEntity(MutableResponse response, Map<String, Object> content) {
        try {
            response.setEntity(objectMapper.writeValueAsBytes(content));
        } catch (JsonProcessingException e) {
            throw new FilterException(e);
        }
    }

    public void replaceEntity(MutableResponse response, List<Map<String, Object>> content) {
        try {
            response.setEntity(objectMapper.writeValueAsBytes(content));
        } catch (JsonProcessingException e) {
            throw new FilterException(e);
        }
    }
}
