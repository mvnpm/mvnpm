package io.mvnpm.npm.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * To handle some Author values that comes in with a String only
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class AuthorDeserializer extends StdDeserializer<Author> {

    public AuthorDeserializer() {
        this(null);
    }

    public AuthorDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Author deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        try {
            return jp.readValueAs(Author.class);
        } catch (Throwable t) {
            // Let try with String
            String name = jp.getValueAsString("invalid");
            return new Author(name);
        }
    }

}
