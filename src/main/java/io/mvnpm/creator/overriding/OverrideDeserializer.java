package io.mvnpm.creator.overriding;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class OverrideDeserializer extends JsonDeserializer<OverrideStore> {

    @Override
    public OverrideStore deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        Map<String, JsonNode> input = parser.readValueAs(new TypeReference<Map<String, JsonNode>>() {
        });

        return new OverrideStore(input);
    }

}
