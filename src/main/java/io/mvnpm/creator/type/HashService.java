package io.mvnpm.creator.type;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.creator.utils.FileUtil;
import io.quarkus.logging.Log;

/**
 * Sign newly created file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class HashService {

    public List<Path> createHashes(Path filePath) {
        List<Path> hashes = new ArrayList<>();
        hashes.add(FileUtil.createSha1(filePath));
        hashes.add(FileUtil.createMd5(filePath));
        Log.debug("file hashes created (sha1 and md5) " + filePath + " [ok]");
        return hashes;
    }
}
