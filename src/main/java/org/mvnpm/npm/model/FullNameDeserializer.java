package org.mvnpm.npm.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class FullNameDeserializer extends StdDeserializer<FullName> {

    public FullNameDeserializer() { 
        this(null); 
    } 

    public FullNameDeserializer(Class<?> vc) { 
        super(vc); 
    }
    
    @Override
    public FullName deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        return FullNameParser.parse(jp.getValueAsString());
    }

}
