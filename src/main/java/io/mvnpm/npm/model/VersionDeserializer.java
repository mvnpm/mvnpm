package io.mvnpm.npm.model;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class VersionDeserializer extends StdDeserializer<Set<String>> {

    public VersionDeserializer() {
        this(null);
    }

    public VersionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Set<String> deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        if (jp.currentToken() != JsonToken.START_OBJECT) {
            return result;
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            result.add(jp.currentName());
            jp.nextToken();
            jp.skipChildren();
        }
        return result;
    }

}
