package io.mvnpm.creator.composite;

import static io.mvnpm.creator.type.JarService.MVNPM_MORE_ARCHIVE;
import static io.mvnpm.creator.type.PomService.resolveDependencies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.importmap.Aggregator;
import io.mvnpm.importmap.ImportsDataBinding;
import io.mvnpm.maven.MavenCentralService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * This group a set of artifacts into one
 *
 * @author Phillip Kruger(phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CompositeCreator {

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    @RestClient
    GitHubService gitHubService;

    @Inject
    EventBus bus;

    @Inject
    Vertx vertx;

    private final AtomicReference<WebClient> webClient = new AtomicReference<>();

    private final Map<String, GitHubContent> compositesMap = new HashMap<>();
    @Inject
    private PomService pomService;
    @Inject
    private MavenCentralService mavenCentralService;

    private WebClient webClient() {
        return webClient.updateAndGet(webClient -> webClient == null ? WebClient.create(vertx) : webClient);
    }

    @PostConstruct
    public void loadAllComposites() {
        compositesMap.clear();
        List<GitHubContent> allComposites = gitHubService.getContents("mvnpm", "composites", "definitions").await()
                .atMost(Duration.of(2, ChronoUnit.MINUTES));

        for (GitHubContent ghc : allComposites) {
            String name = ghc.getName();
            name = name.replace(".xml", "");
            compositesMap.put(name, ghc);
        }
    }

    public Path getOrBuildComposite(String artifactId, String version) {
        if (this.compositesMap.containsKey(artifactId)) {
            String url = this.compositesMap.get(artifactId).getDownloadUrl();
            return build(url, version);
        } else {
            throw new RuntimeException("Composite definition for " + artifactId + " does not exist");
        }
    }

    public Path getImportMapPath(Name name, String version) {
        return packageFileLocator.getLocalDirectory(name, version).resolve("importmap.json");
    }

    private Path build(String downloadUrl, String version) {
        try {
            String content = downloadFileContent(downloadUrl);
            Model model = pomService.readPom(content);
            if (version == null) {
                version = getLatestVersionOfFirstDependency(model); // Used in auto update
            }
            if (version != null) {
                model.setVersion(version);
                List<Dependency> dependencies = resolveDependencies(model);
                try {
                    return merge(model, dependencies);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    private String getLatestVersionOfFirstDependency(Model model) {
        List<Dependency> dependencies = model.getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            Dependency first = dependencies.get(0);
            Name name = NameParser.fromMavenGA(first.getGroupId(), first.getArtifactId());
            Project p = npmRegistryFacade.getProject(name.npmFullName);
            return p.distTags().latest();
        }
        return null;
    }

    public Collection<GitHubContent> listComposites() {
        return this.compositesMap.values();
    }

    private Path merge(Model model, List<Dependency> dependencies) throws IOException {
        Path jar = packageFileLocator.getLocalFullPath(FileType.jar, model.getGroupId(), model.getArtifactId(),
                model.getVersion());
        Path sourceJar = packageFileLocator.getLocalFullPath(FileType.source, model.getGroupId(), model.getArtifactId(),
                model.getVersion());
        if (!Files.exists(jar) || !Files.exists(sourceJar)) {
            final Path outputPom = getPomPath(jar);
            Name name = NameParser.fromMavenGA(model.getGroupId(), model.getArtifactId());
            Model pom = mergeJar(jar, outputPom, model, dependencies);
            mergeSource(sourceJar, pom, dependencies);
            bus.send(NewJarEvent.EVENT_NAME,
                    new NewJarEvent(outputPom, jar, null, List.of(sourceJar), name, pom.getVersion()));

        }
        return jar;

    }

    private Model mergeJar(Path outputJar, Path outputPom, Model model, List<Dependency> dependencies)
            throws IOException {
        Model pom = model.clone();
        Files.createDirectories(outputJar.getParent());
        // Create a new JAR file to merge the others into
        final Path tempJar = FileUtil.getTempFilePathFor(outputJar);
        try (OutputStream jarFile = Files.newOutputStream(tempJar);
                JarOutputStream mergedJar = new JarOutputStream(jarFile);
                ByteArrayOutputStream commonTgzBaos = new ByteArrayOutputStream();
                GZIPOutputStream commonTgzGzos = new GZIPOutputStream(commonTgzBaos);
                TarArchiveOutputStream commonTgzOut = new TarArchiveOutputStream(commonTgzGzos)) {

            commonTgzOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            // Collect all importmaps to merge at the end
            Map<String, String> importmaps = new HashMap<>();
            // Create new merged pom.xml
            pom.getDependencies().clear();
            Map<String, Dependency> newDependencies = new HashMap<>();
            Map<String, Developer> newDevelopers = new HashMap<>();
            Map<String, License> newLicenses = new HashMap<>();
            int countMvnpmMoreTgz = 0;

            for (Dependency dependency : dependencies) {
                Name jarName = NameParser.fromMavenGA(dependency.getGroupId(), dependency.getArtifactId());
                final Path jarPath = getJar(dependency, jarName, FileType.jar);

                try (InputStream inputStream = Files.newInputStream(jarPath);
                        JarInputStream inputJar = new JarInputStream(inputStream)) {
                    // Add all entries from the input JAR to the merged JAR
                    JarEntry entry;
                    while ((entry = inputJar.getNextJarEntry()) != null) {
                        String entryName = entry.getName();

                        if ("META-INF/importmap.json".equals(entryName)) {
                            // Remember importmap
                            importmaps.putAll(getImportMap(inputJar));
                        } else if (entryName.startsWith("META-INF/maven")) {
                            updatePom(inputJar, newDependencies, newDevelopers, newLicenses, entryName,
                                    List.copyOf(mapByGA(dependencies).keySet()));
                        } else if (entryName.startsWith(MVNPM_MORE_ARCHIVE)) {
                            countMvnpmMoreTgz++;
                            extractTgzEntriesAndMergeToCommon(inputJar, commonTgzOut);
                        } else if (!entryName.endsWith("LICENSE")) {
                            writeEntry(inputJar, mergedJar, entry);
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    throw new RuntimeException("Error processing JAR entry: " + jarPath, e);
                }
            }

            commonTgzOut.finish();
            commonTgzGzos.finish();
            commonTgzBaos.flush();
            if (countMvnpmMoreTgz > 0 && commonTgzBaos.size() > 0) {
                writeEntry(mergedJar, MVNPM_MORE_ARCHIVE, commonTgzBaos.toByteArray());
            }

            // Add importmap (in jar and on disk)
            Aggregator aggregator = new Aggregator(importmaps);
            String aggregatedImportMap = aggregator.aggregateAsJson(false);
            writeEntry(mergedJar, "META-INF/importmap.json", aggregatedImportMap);
            Path importmapPath = outputJar.getParent().resolve("importmap.json");
            packageFileLocator.createFileAtomic(importmapPath, aggregatedImportMap.getBytes());

            // Add pom (in jar and on disk)
            pom.setDependencies(List.copyOf(newDependencies.values()));
            pom.setDevelopers(List.copyOf(newDevelopers.values()));
            pom.setLicenses(List.copyOf(newLicenses.values()));
            MavenXpp3Writer mxw = new MavenXpp3Writer();

            FileUtil.createDirectories(outputPom);
            try (StringWriter stringWriter = new StringWriter()) {
                mxw.write(stringWriter, pom);
                FileUtil.writeAtomic(outputPom, stringWriter.toString());
                writeEntry(mergedJar, "META-INF/maven/" + pom.getGroupId() + "/" + pom.getArtifactId() + "/pom.xml",
                        outputPom);

            }

            // Add pom.properties
            Properties properties = new Properties();
            properties.setProperty("groupId", pom.getGroupId());
            properties.setProperty("artifactId", pom.getArtifactId());
            properties.setProperty("version", pom.getVersion());
            try (StringWriter writer = new StringWriter()) {
                properties.store(writer, "Generated by mvnpm.org");
                writeEntry(mergedJar, "META-INF/maven/" + pom.getGroupId() + "/" + pom.getArtifactId() + "/pom.properties",
                        writer.toString());
            }

            Log.info(pom.getGroupId() + ":" + pom.getArtifactId() + ":" + pom.getVersion() + " created");

        } catch (IOException e) {
            throw new RuntimeException("Error creating JAR output: " + outputJar, e);
        }
        FileUtil.forceMoveAtomic(tempJar, outputJar);
        return pom;
    }

    private Path getJar(Dependency dependency, Name jarName, FileType type) {
        try {
            return mavenRepositoryService.getPath(jarName, dependency.getVersion(), type);
        } catch (PackageAlreadySyncedException e) {
            return mavenCentralService.downloadFromMavenCentral(jarName, dependency.getVersion(), type);
        }
    }

    private static Path getPomPath(Path outputJar) {
        String jarName = outputJar.toString();
        String pomName = jarName.substring(0, jarName.length() - 3) + "pom";
        Path outputPom = Path.of(pomName);
        return outputPom;
    }

    private void extractTgzEntriesAndMergeToCommon(JarInputStream inputJar, TarArchiveOutputStream commonTgz)
            throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(inputJar, baos);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(bais);
                    TarArchiveInputStream tgzIn = new TarArchiveInputStream(gzipIn)) {
                TarArchiveEntry tgzEntry;
                while ((tgzEntry = tgzIn.getNextTarEntry()) != null) {
                    commonTgz.putArchiveEntry(tgzEntry);
                    IOUtils.copy(tgzIn, commonTgz);
                    commonTgz.closeArchiveEntry();
                }
            }
        }
    }

    private void mergeSource(Path outputSourceJar, Model pom, List<Dependency> dependencies)
            throws IOException {
        Files.createDirectories(outputSourceJar.getParent());
        final Path tempJar = FileUtil.getTempFilePathFor(outputSourceJar);
        // Create a new JAR file to merge the others into
        try (OutputStream jarFile = Files.newOutputStream(tempJar);
                JarOutputStream mergedJar = new JarOutputStream(jarFile)) {

            // Create new merged pom.xml
            for (Dependency dependency : dependencies) {
                Name jarName = NameParser.fromMavenGA(dependency.getGroupId(), dependency.getArtifactId());
                Path jarPath = getJar(dependency, jarName, FileType.source);

                try (InputStream inputStream = Files.newInputStream(jarPath);
                        JarInputStream inputJar = new JarInputStream(inputStream)) {
                    // Add all entries from the input JAR to the merged JAR
                    JarEntry entry;
                    while ((entry = inputJar.getNextJarEntry()) != null) {
                        writeEntry(inputJar, mergedJar, dependency.getArtifactId(), entry);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Add pom (in jar)
            writeEntry(mergedJar, "META-INF/maven/" + pom.getGroupId() + "/" + pom.getArtifactId() + "/pom.xml",
                    modelToString(pom));

            // Add pom.properties
            Properties p = new Properties();
            p.setProperty("groupId", pom.getGroupId());
            p.setProperty("artifactId", pom.getArtifactId());
            p.setProperty("version", pom.getVersion());
            try (StringWriter writer = new StringWriter()) {
                p.store(writer, "Generated by mvnpm.org");
                String pomProperties = writer.toString();
                writeEntry(mergedJar, "META-INF/maven/" + pom.getGroupId() + "/" + pom.getArtifactId() + "/pom.properties",
                        pomProperties);
            }

            Log.info(pom.getGroupId() + ":" + pom.getArtifactId() + ":" + pom.getVersion() + " source created");
        }
        FileUtil.forceMoveAtomic(tempJar, outputSourceJar);
    }

    private String modelToString(Model model) {
        try {
            StringWriter stringWriter = new StringWriter();
            MavenXpp3Writer modelWriter = new MavenXpp3Writer();
            modelWriter.write(stringWriter, model);
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updatePom(JarInputStream inputJar,
            Map<String, Dependency> newDependencies,
            Map<String, Developer> newDevelopers,
            Map<String, License> newLicences,
            String name,
            List<String> ignoreListGA) throws IOException, XmlPullParserException {
        if (name.endsWith("pom.xml")) {
            String jarPom = getEntryContent(inputJar);
            final Model jarPomModel = pomService.readPom(jarPom);
            List<Dependency> resolveDependencies = resolveDependencies(jarPomModel);
            Map<String, Dependency> thisJarDeps = mapByGA(resolveDependencies);
            for (String ga : thisJarDeps.keySet()) {
                if (!ignoreListGA.contains(ga)) {
                    newDependencies.put(ga, thisJarDeps.get(ga));
                }
            }

            for (Developer jarDev : jarPomModel.getDevelopers()) {
                if (!newDevelopers.containsKey(jarDev.toString())) {
                    newDevelopers.put(jarDev.toString(), jarDev);
                }
            }

            for (License jarLic : jarPomModel.getLicenses()) {
                if (!newLicences.containsKey(jarLic.toString())) {
                    newLicences.put(jarLic.toString(), jarLic);
                }
            }
        }
    }

    private Map<String, Dependency> mapByGA(List<Dependency> l) {
        Map<String, Dependency> m = new HashMap<>();
        for (Dependency d : l) {
            m.put(d.getGroupId() + ":" + d.getArtifactId(), d);
        }
        return m;
    }

    private void writeEntry(JarOutputStream jar, String name, String content) throws IOException {
        writeEntry(jar, name, content.getBytes());
    }

    private void writeEntry(JarOutputStream jar, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name);
        jar.putNextEntry(entry);
        jar.write(bytes);
        jar.closeEntry();
    }

    private void writeEntry(JarOutputStream mergedJar, String name, Path path) throws IOException {
        JarEntry entry = new JarEntry(name);
        mergedJar.putNextEntry(entry);
        try (InputStream fileInputStream = Files.newInputStream(path)) {
            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                mergedJar.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error jarring file content for " + path, e);
        }
        mergedJar.closeEntry();
    }

    private void writeEntry(JarInputStream inputJar, JarOutputStream mergedJar, JarEntry entry) throws IOException {
        mergedJar.putNextEntry(entry);

        // Read the content of the entry and write it to the merged JAR
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputJar.read(buffer)) != -1) {
            mergedJar.write(buffer, 0, bytesRead);
        }
        mergedJar.closeEntry();
    }

    private void writeEntry(JarInputStream inputJar, JarOutputStream mergedJar, String root, JarEntry entry)
            throws IOException {
        // Create a new entry with the desired name
        String newEntryName = root + entry.getName();
        JarEntry newEntry = new JarEntry(newEntryName);
        mergedJar.putNextEntry(newEntry);

        // Read the content of the entry and write it to the merged JAR
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputJar.read(buffer)) != -1) {
            mergedJar.write(buffer, 0, bytesRead);
        }
        mergedJar.closeEntry();
    }

    private Map<String, String> getImportMap(JarInputStream inputJar) throws IOException {
        String json = getEntryContent(inputJar);
        return ImportsDataBinding.toImports(json).getImports();
    }

    public String getEntryContent(JarInputStream inputJar) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Read the content of the entry and write it to a String
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputJar.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] bytes = baos.toByteArray();
            return new String(bytes);
        }
    }

    private String downloadFileContent(String downloadUrl) throws InterruptedException {
        final HttpResponse<Buffer> response = webClient().getAbs(downloadUrl)
                .send()
                .await().indefinitely();
        return response.bodyAsString();
    }
}
