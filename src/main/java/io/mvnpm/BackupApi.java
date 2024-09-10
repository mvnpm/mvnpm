package io.mvnpm;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.NoCache;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.quarkus.logging.Log;

/**
 * Export/Import saved data
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api/backup")
public class BackupApi {

    @Inject
    CentralSyncItemService centralSyncItemService;

    @GET
    @NoCache
    public List<CentralSyncItem> exportAll() {
        return CentralSyncItem.findAll().list();
    }

    @POST
    @Consumes(value = "application/json")
    public void importAll(List<CentralSyncItem> data) {
        data.forEach((d) -> {
            centralSyncItemService.merge(d);
            Log.info(d.toGavString() + " added.");
        });

    }

}
