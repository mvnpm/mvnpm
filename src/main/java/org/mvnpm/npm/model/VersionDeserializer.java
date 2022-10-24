package org.mvnpm.npm.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.quarkus.logging.Log;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VersionDeserializer extends StdDeserializer<Set<String>> {

    public VersionDeserializer() { 
        this(null); 
    } 

    public VersionDeserializer(Class<?> vc) { 
        super(vc); 
    }
    
    @Override
    public Set<String> deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        Map v = jp.readValueAs(Map.class);
        return v.keySet();
    }

}
