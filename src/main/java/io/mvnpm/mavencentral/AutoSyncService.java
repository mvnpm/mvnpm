package io.mvnpm.mavencentral;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.mavencentral.sync.ContinuousSyncService;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;

/**
 * Once a new jar file is created, we need to kick of the sync process
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AutoSyncService {

    @Inject
    ContinuousSyncService continuousSyncService;

    public void triggerSync(Name name, String version) {
        boolean queued = continuousSyncService.initializeSync(name, version);
        if (queued) {
            Log.info(name.displayName + " " + version + " added to the sync queue");
        }
    }

}
