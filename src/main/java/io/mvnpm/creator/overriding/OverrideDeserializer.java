package io.mvnpm.creator.overriding;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import io.mvnpm.npm.model.Name;

public class OverrideDeserializer extends JsonDeserializer<Map<Name, String>> {

    @Override
    public Map<Name, String> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        Map<String, JsonNode> input = parser.readValueAs(new TypeReference<Map<String, JsonNode>>() {
        });

        final OverrideStore store = new OverrideStore(input);
        store.process();
        return store.unload();
    }

}
