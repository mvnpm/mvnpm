package io.mvnpm.mavencentral.sync;

import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

/**
 * Websocket on the Sync queue
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * TODO: Add progress and date
 * TODO: Add initial state on connection
 */
@Path("/api/sync")
@ServerEndpoint("/api/queue/")         
@ApplicationScoped
public class CentralSyncApi {
 
    @Inject
    CentralSyncService centralSyncService;
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
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
            s.getAsyncRemote().sendObject(centralSyncItem, result ->  {
                if (result.getException() != null) {
                    Log.error("Unable to send message: " + result.getException());
                }
            });
        });
    }
    
    @ConsumeEvent("central-sync-item-stage-change")
    public void uploading(CentralSyncItem centralSyncItem) {
        broadcast(centralSyncItem);
    }
    
    @GET
    @Path("/info/{groupId}/{artifactId}")
    public SyncInfo syncInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @DefaultValue("latest") @QueryParam("version") String version ) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        if(version.equalsIgnoreCase("latest")){
            version = getLatestVersion(name);
        }
        return centralSyncService.getSyncInfo(groupId, artifactId, version);
    }
    
    private String getLatestVersion(Name fullName){
        Project project = npmRegistryFacade.getProject(fullName.npmFullName());
        return project.distTags().latest();
    }
}