package io.mvnpm.npm.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class NameDeserializer extends StdDeserializer<Name> {

    public NameDeserializer() { 
        this(null); 
    } 

    public NameDeserializer(Class<?> vc) { 
        super(vc); 
    }
    
    @Override
    public Name deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        return NameParser.fromNpmProject(jp.getValueAsString());
    }

}
