package io.mvnpm;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.NoCache;

import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.maven.sync.SyncItemService;
import io.quarkus.logging.Log;

/**
 * Export/Import saved data
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api/backup")
public class BackupApi {

    @Inject
    SyncItemService syncItemService;

    @GET
    @NoCache
    public List<SyncItem> exportAll() {
        return SyncItem.findAll().list();
    }

    @POST
    @Consumes(value = "application/json")
    public void importAll(List<SyncItem> data) {
        data.forEach((d) -> {
            syncItemService.merge(d);
            Log.info(d.toGavString() + " added.");
        });

    }

}
