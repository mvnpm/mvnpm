package io.mvnpm.log;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encoding the event log item
 *
 * @author Phillip Kruger(phillip.kruger@gmail.com)
 */
public class EventLogEncoder implements Encoder.Text<EventLogEntry>, Decoder.Text<EventLogEntry> {

    private final ObjectMapper objectMapper;

    public EventLogEncoder() {
        this.objectMapper = CDI.current().select(ObjectMapper.class).get();
    }

    @Override
    public String encode(EventLogEntry object) throws EncodeException {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new EncodeException(object, "Error encoding EventLogEntry to Json", ex);
        }
    }

    @Override
    public EventLogEntry decode(String s) throws DecodeException {
        try {
            EventLogEntry o = objectMapper.readValue(s, EventLogEntry.class);
            return o;
        } catch (JsonProcessingException ex) {
            throw new DecodeException(s, "Error decoding json to EventLogEntry", ex);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
