package io.mvnpm.npm.model;

import java.io.IOException;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * To handle special characters in description
 *
 */
public class DescriptionDeserializer extends StdDeserializer<String> {
    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[\\x00-\\x1F\\x7F]");

    public DescriptionDeserializer() {
        this(null);
    }

    public DescriptionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        String value = jp.getValueAsString();
        if (value == null) {
            return null;
        }

        return INVALID_CHAR_PATTERN.matcher(value).replaceAll("");
    }

}
