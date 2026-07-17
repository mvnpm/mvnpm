<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncApi.java
package io.mvnpm.maven.sync;
========
package io.mvnpm.maven.api;
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncApi.java

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
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.Stage;
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
@ServerEndpoint(value = "/api/queue/", encoders = SyncItemEncoder.class, decoders = SyncItemEncoder.class)
@ApplicationScoped
public class SyncApi {

    @Inject
    SyncService syncService;
<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncApi.java

    @Inject
    private SyncItemService syncItemService;
========
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncApi.java

    @Inject
    private MavenRepositoryService mavenRepositoryService;

    private final Set<Session> sessions = new ConcurrentHashSet<>();
<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncApi.java
========

    @Inject
    private SyncItemService syncItemService;
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncApi.java

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

    private void broadcast(SyncItem syncItem) {
        sessions.forEach(s -> {
            s.getAsyncRemote().sendObject(syncItem, result -> {
                if (result.getException() != null) {
                    Log.error("Unable to send message: " + result.getException());
                    sessions.remove(s);
                }
            });
        });
    }

    @ConsumeEvent("sync-item-stage-change")
    @Blocking
    public void stateChange(SyncItem syncItem) {
        broadcast(syncItem);
    }

    @GET
    @NoCache
    @Path("/info/{groupId}/{artifactId}")
<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncApi.java
    public SyncItem getSyncItem(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
========
    public SyncItem getCentralSyncItem(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncApi.java
            @DefaultValue("latest") @QueryParam("version") String version) {
        return syncService.checkReleaseInDbAndRepo(groupId, artifactId, version, false);
    }

    @GET
    @NoCache
    @Path("/request/{groupId}/{artifactId}")
    public SyncItem requestFullSync(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        mavenRepositoryService.getPath(groupId, artifactId, version, FileType.jar);
        return syncService.checkReleaseInDbAndRepo(groupId, artifactId, version, true);
    }

    @GET
    @NoCache
    @Path("/retry/{groupId}/{artifactId}")
    public SyncItem retryFullSync(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {

        if (version.equalsIgnoreCase("latest")) {
            version = syncService.getLatestVersion(groupId, artifactId);
        }

        mavenRepositoryService.getPath(groupId, artifactId, version, FileType.jar);
<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncApi.java
        final SyncItem syncItem = syncService.checkReleaseInDbAndRepo(groupId, artifactId, version, true);
        if (syncItem.isInError()) {
            SyncItem claimed = syncItemService
                    .claimForErrorRetry(new Gav(syncItem.groupId, syncItem.artifactId, syncItem.version));
========
        final SyncItem centralSyncItem = syncService.checkReleaseInDbAndRepo(groupId, artifactId, version,
                true);
        if (centralSyncItem.isInError()) {
            SyncItem claimed = syncItemService.claimForErrorRetry(
                    new Gav(centralSyncItem.groupId, centralSyncItem.artifactId, centralSyncItem.version));
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncApi.java
            if (claimed != null) {
                return claimed;
            }
        }
        return syncItem;

    }

    @GET
    @NoCache
    @Path("/remove/{groupId}/{artifactId}")
    @Transactional
    public SyncItem remove(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        if (version.equalsIgnoreCase("latest")) {
            version = syncService.getLatestVersion(groupId, artifactId);
        }
<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncApi.java
        final SyncItem syncItem = syncService.checkReleaseInDbAndRepo(groupId, artifactId, version, false);
        syncItem.delete();
        return syncItem;
========
        final SyncItem centralSyncItem = syncService.checkReleaseInDbAndRepo(groupId, artifactId, version,
                false);
        centralSyncItem.delete();
        return centralSyncItem;
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncApi.java
    }

    @GET
    @NoCache
    @Path("/item/{stage}")
    public List<SyncItem> getItems(@PathParam("stage") Stage stage) {
        return SyncItem.findByStage(stage, 150);
    }

    @GET
    @NoCache
    @Path("/items")
    public List<SyncItem> getItems() {
        return SyncItem.findAll().list();
    }

}
