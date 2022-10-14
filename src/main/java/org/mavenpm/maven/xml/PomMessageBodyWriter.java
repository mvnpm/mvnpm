package org.mavenpm.maven.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * Converts a npm package to a pom
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * // TODO: Add OpenAPI annotations for example
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class PomMessageBodyWriter implements MessageBodyWriter<org.mavenpm.npm.model.Package>{

    @Inject
    PomCreator pomCreator;
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == org.mavenpm.npm.model.Package.class;
    }

    @Override
    public void writeTo(org.mavenpm.npm.model.Package npmpackage, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        pomCreator.writeTo(npmpackage, entityStream);
    }
    
}
