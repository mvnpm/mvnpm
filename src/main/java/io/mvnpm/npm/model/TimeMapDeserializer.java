package io.mvnpm.npm.model;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializes the NPM "time" map, skipping entries whose values are not strings
 * (e.g. the "unpublished" key which contains an object).
 */
public class TimeMapDeserializer extends StdDeserializer<Map<String, String>> {

    public TimeMapDeserializer() {
        this(null);
    }

    public TimeMapDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Map<String, String> deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        if (jp.currentToken() != JsonToken.START_OBJECT) {
            return result;
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String key = jp.currentName();
            jp.nextToken();
            if (jp.currentToken() == JsonToken.VALUE_STRING) {
                result.put(key, jp.getValueAsString());
            } else {
                jp.skipChildren();
            }
        }
        return result;
    }
}