package io.mvnpm.mavencentral.sync;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
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
    NpmRegistryFacade npmRegistryFacade;
    @Inject
    ContinuousSyncService continuousSyncService;

    private Set<Session> sessions = new ConcurrentHashSet<>();

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
    @Path("/info/{groupId}/{artifactId}")
    public SyncInfo syncInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        if (version.equalsIgnoreCase("latest")) {
            version = getLatestVersion(name);
        }
        return centralSyncService.getSyncInfo(name, version);
    }

    @GET
    @Path("/initQueue")
    public List<CentralSyncItem> getInitQueue() {
        return continuousSyncService.getInitQueue();
    }

    @GET
    @Path("/uploadingQueue")
    public List<CentralSyncItem> getUploadingQueue() {
        return continuousSyncService.getUploadInProgress();
    }

    @GET
    @Path("/uploadedQueue")
    public List<CentralSyncItem> getUploadedQueue() {
        return continuousSyncService.getClosedQueue();
    }

    @GET
    @Path("/closedQueue")
    public List<CentralSyncItem> getClosedQueue() {
        return continuousSyncService.getReleaseQueue();
    }

    @GET
    @Path("/releasingQueue")
    public List<CentralSyncItem> getReleasingQueue() {
        return continuousSyncService.getReleasedQueue();
    }

    private String getLatestVersion(Name fullName) {
        Project project = npmRegistryFacade.getProject(fullName.npmFullName());
        return project.distTags().latest();
    }
}
