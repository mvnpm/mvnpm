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
    @Inject
    CentralSyncStageService centralSyncStageService;

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
    public CentralSyncItem getCentralSyncItem(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        if (version.equalsIgnoreCase("latest")) {
            version = getLatestVersion(name);
        }

        CentralSyncItem stored = CentralSyncItem.findByGAV(groupId, artifactId, version);
        if (stored != null)
            return stored;

        // Check the status
        CentralSyncItem csi = new CentralSyncItem(name, version);
        if (centralSyncService.isInMavenCentralRemoteCheck(csi)) {
            csi.stage = Stage.RELEASED;
            centralSyncStageService.merge(csi);
        }
        return csi;
    }

    @GET
    @Path("/request/{groupId}/{artifactId}")
    public CentralSyncItem requestFullSync(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        if (version.equalsIgnoreCase("latest")) {
            version = getLatestVersion(name);
        }

        CentralSyncItem stored = CentralSyncItem.findByGAV(groupId, artifactId, version);
        if (stored != null && (stored.isInProgress() || stored.stage.equals(Stage.INIT)))
            return stored;

        // Check the remote status
        CentralSyncItem csi = new CentralSyncItem(name, version);
        if (centralSyncService.isInMavenCentralRemoteCheck(csi)) {
            csi.stage = Stage.RELEASED;
            centralSyncStageService.merge(csi);
            return csi;
        }

        // Else kick off sync
        // TODO: We need to resolve version ranges before we can do this.
        //        Set<Map.Entry<Name, String>> deps = null;
        //        try {
        //            Package npmPackage = npmRegistryFacade.getPackage(name.npmFullName, version);
        //            deps = npmPackage.dependencies().entrySet();
        //        }catch (WebApplicationException wae){
        //            Log.error("Could not kick off sync of dependencendies " + wae.getMessage());
        //        }
        continuousSyncService.initializeSync(name, version);
        // Also request sync for dependencies.
        //        if(deps!=null){
        //            for (Map.Entry<Name, String> dep : deps) {
        //                requestFullSync(dep.getKey().mvnGroupId, dep.getKey().mvnArtifactId, dep.getValue());
        //            }
        //        }
        return CentralSyncItem.findByGAV(groupId, artifactId, version);
    }

    @GET
    @Path("/item/{stage}")
    public List<CentralSyncItem> getItems(@PathParam("stage") Stage stage) {
        return CentralSyncItem.findByStage(stage);
    }

    @GET
    @Path("/items")
    public List<CentralSyncItem> getItems() {
        return CentralSyncItem.findAll().list();
    }

    private String getLatestVersion(Name fullName) {
        Project project = npmRegistryFacade.getProject(fullName.npmFullName);
        return project.distTags().latest();
    }
}
