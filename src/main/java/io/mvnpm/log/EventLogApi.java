package io.mvnpm.log;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.impl.ConcurrentHashSet;

/**
 * Websocket on the event log
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api/eventlog")
@ServerEndpoint(value = "/api/stream/eventlog", encoders = EventLogEncoder.class, decoders = EventLogEncoder.class)
@ApplicationScoped
public class EventLogApi {

    private final Set<Session> sessions = new ConcurrentHashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        // Send current
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
        sessions.remove(session);
    }

    private void broadcast(EventLogEntry eventLogEntry) {
        sessions.forEach(s -> {
            s.getAsyncRemote().sendObject(eventLogEntry, result -> {
                if (result.getException() != null) {
                    Log.error("Unable to send message: " + result.getException());
                }
            });
        });
    }

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    @Transactional
    public void stateChange(CentralSyncItem centralSyncItem) {
        EventLogEntry eventLogEntry = EventLogEntryUtil.toEventLogEntry(centralSyncItem);
        eventLogEntry.persist();
        broadcast(eventLogEntry);
    }

    @ConsumeEvent("exception-in-code")
    @Blocking
    @Transactional
    public void exception(EventLogEntry eventLogEntry) {
        eventLogEntry.persist();
        broadcast(eventLogEntry);
    }

    @GET
    @Path("/top")
    public List<EventLogEntry> getTop(@QueryParam("limit") @DefaultValue("999") int limit) {
        return EventLogEntry.findAll(Sort.by("time", Sort.Direction.Descending)).range(0, limit).list();
    }

    @GET
    @Path("/gav/{groupId}/{artifactId}/{version}")
    public List<EventLogEntry> getGavLog(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @PathParam("version") String version) {
        return EventLogEntry.findByGav(groupId, artifactId, version);
    }

    @Transactional
    public void clearLog() {
        EventLogEntry.deleteAll();
    }
}
