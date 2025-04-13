package io.mvnpm.mavencentral.sync;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

import org.jboss.resteasy.reactive.NoCache;

import io.mvnpm.creator.FileType;
import io.mvnpm.maven.MavenRepositoryService;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.impl.ConcurrentHashSet;

/**
 * Websocket on the Sync queue
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api/sync")
@ServerEndpoint(value = "/api/queue/", encoders = CentralSyncItemEncoder.class, decoders = CentralSyncItemEncoder.class)
@ApplicationScoped
public class CentralSyncApi {

    @Inject
    CentralSyncService centralSyncService;

    @Inject
    private MavenRepositoryService mavenRepositoryService;

    private final Set<Session> sessions = new ConcurrentHashSet<>();
    @Inject
    private CentralSyncItemService centralSyncItemService;

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

    private void broadcast(CentralSyncItem centralSyncItem) {
        sessions.forEach(s -> {
            s.getAsyncRemote().sendObject(centralSyncItem, result -> {
                if (result.getException() != null) {
                    Log.error("Unable to send message: " + result.getException());
                    sessions.remove(s);
                }
            });
        });
    }

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    public void stateChange(CentralSyncItem centralSyncItem) {
        broadcast(centralSyncItem);
    }

    @GET
    @NoCache
    @Path("/info/{groupId}/{artifactId}")
    public CentralSyncItem getCentralSyncItem(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        return centralSyncService.checkReleaseInDbAndCentral(groupId, artifactId, version, false);
    }

    @GET
    @NoCache
    @Path("/request/{groupId}/{artifactId}")
    public CentralSyncItem requestFullSync(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        mavenRepositoryService.getPath(groupId, artifactId, version, FileType.jar);
        return centralSyncService.checkReleaseInDbAndCentral(groupId, artifactId, version, true);
    }

    @GET
    @NoCache
    @Path("/retry/{groupId}/{artifactId}")
    public CentralSyncItem retryFullSync(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {

        if (version.equalsIgnoreCase("latest")) {
            version = centralSyncService.getLatestVersion(groupId, artifactId);
        }

        mavenRepositoryService.getPath(groupId, artifactId, version, FileType.jar);
        final CentralSyncItem centralSyncItem = centralSyncService.checkReleaseInDbAndCentral(groupId, artifactId, version,
                true);
        if (centralSyncItem.isInError()) {
            return centralSyncItemService.tryErroredItemAgain(centralSyncItem);
        }
        return centralSyncItem;

    }

    @GET
    @NoCache
    @Path("/remove/{groupId}/{artifactId}")
    @Transactional
    public CentralSyncItem remove(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {

        if (version.equalsIgnoreCase("latest")) {
            version = centralSyncService.getLatestVersion(groupId, artifactId);
        }

        mavenRepositoryService.getPath(groupId, artifactId, version, FileType.jar);
        final CentralSyncItem centralSyncItem = centralSyncService.checkReleaseInDbAndCentral(groupId, artifactId, version,
                false);
        centralSyncItem.delete();
        return centralSyncItem;
    }

    @GET
    @NoCache
    @Path("/item/{stage}")
    public List<CentralSyncItem> getItems(@PathParam("stage") Stage stage) {
        return CentralSyncItem.findByStage(stage, 150);
    }

    @GET
    @NoCache
    @Path("/items")
    public List<CentralSyncItem> getItems() {
        return CentralSyncItem.findAll().list();
    }

}
