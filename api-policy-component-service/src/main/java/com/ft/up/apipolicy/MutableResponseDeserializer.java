package com.ft.up.apipolicy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is a custom Jackson deserializer for {@link com.ft.up.apipolicy.pipeline.MutableResponse} bodies.
 * It works in the following way:
 * <p>
 * If the response body is a json array convert it to a {@link Map} with the key being the index of the current json
 * array object and the value being the object itself.
 * </p>
 * <p>
 * If the response body is a json object convert it directly to a {@link Map}.
 * </p>
 * This is done in order to preserve the {@link Map} as a common deserialization structure in both cases.
 *
 * @author Tsvetan Dimitrov (tsvetan.dimitrov@ft.com)
 */
public class MutableResponseDeserializer extends JsonDeserializer<Map<String, Object>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<LinkedHashMap<String, Object>> JSON_MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {
    };

    @Override
    public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode rootNode = MAPPER.readTree(jsonParser);
        if (rootNode.isArray()) {
            Map<String, Object> listMap = new LinkedHashMap<>();
            int count = 0;
            for (JsonNode node : rootNode) {
                Map<String, Object> map = toMap(node);
                String key = String.valueOf(count++);
                listMap.put(key, map);
            }
            return listMap;
        }
        return toMap(rootNode);
    }

    private Map<String, Object> toMap(JsonNode node) {
        return MAPPER.convertValue(node, JSON_MAP_TYPE);
    }
}
