package io.mvnpm.composite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileType;
import io.mvnpm.file.FileUtil;
import io.mvnpm.importmap.Aggregator;
import io.mvnpm.importmap.ImportsDataBinding;
import io.mvnpm.maven.MavenRespositoryService;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;

/**
 * This group a set of artifacts into one
 *
 * @author Phillip Kruger(phillip.kruger@gmail.com)
 *
 */
@ApplicationScoped
public class CompositeCreator {

    @ConfigProperty(name = "mvnpm.composites-directory")
    String compositesDirectory;

    @Inject
    FileStore fileStore;

    @Inject
    MavenRespositoryService mavenRespositoryService;

    private final MavenXpp3Reader mavenXpp3Writer = new MavenXpp3Reader();

    @Blocking
    public void buildAllComposites() {
        Set<Path> poms = getAllCompositePoms();
        for (Path pom : poms) {
            build(pom, null); // TODO: Get latest version
        }
    }

    @Blocking
    public void buildComposite(String artifactId, String version) {
        Path compositesFolder = Paths.get(compositesDirectory);
        Path pom = compositesFolder.resolve(artifactId + ".xml");
        if (Files.exists(pom)) {
            build(pom, version);
        } else {
            throw new RuntimeException("Composite definition for " + artifactId + " does not exist");
        }
    }

    public Path getImportMapPath(Name name, String version) {
        return fileStore.getLocalDirectory(name, version).resolve("importmap.json");
    }

    private void build(Path pom, String version) {
        try (InputStream inputStream = Files.newInputStream(pom)) {
            Model model = mavenXpp3Writer.read(inputStream);
            if (version != null) {
                model.setVersion(version);
            }
            List<Dependency> dependencies = resolveDependencies(model);
            merge(model, dependencies);
        } catch (XmlPullParserException ex) {
            throw new RuntimeException("Invalid pom xml for " + pom);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Set<Path> getAllCompositePoms() {
        Path compositesFolder = Paths.get(compositesDirectory);
        boolean hasComposites = Files.exists(compositesFolder);
        if (hasComposites) {
            try (Stream<Path> stream = Files.list(compositesFolder)) {
                return stream
                        .filter(file -> !Files.isDirectory(file))
                        .collect(Collectors.toSet());
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return Set.of();
    }

    private List<Dependency> resolveDependencies(Model model) {
        Properties properties = model.getProperties();
        List<Dependency> resolvedDependencies = new ArrayList<>();

        List<Dependency> dependencies = model.getDependencies();

        for (Dependency dependency : dependencies) {
            String version = dependency.getVersion();
            if (version != null && version.startsWith("${") && version.endsWith("}")) {
                // If the version is a property reference, resolve it
                String propertyName = version.substring(2, version.length() - 1);
                String resolvedVersion = resolveVersion(properties, propertyName, model.getVersion());

                if (resolvedVersion != null) {
                    dependency.setVersion(resolvedVersion);
                }
            }
            resolvedDependencies.add(dependency);
        }
        return resolvedDependencies;
    }

    private String resolveVersion(Properties properties, String propertyName, String projectVersion) {
        if (propertyName.equals("project.version")) {
            return projectVersion;
        } else {
            return properties.getProperty(propertyName);
        }
    }

    private void merge(Model model, List<Dependency> dependencies) throws IOException, XmlPullParserException {
        Model pom = mergeJar(model, dependencies);
        mergeSource(pom, dependencies);
    }

    private Model mergeJar(Model model, List<Dependency> dependencies) throws IOException, XmlPullParserException {
        Model pom = model.clone();
        Path outputJar = fileStore.getLocalFullPath(FileType.jar, model.getGroupId(), model.getArtifactId(),
                model.getVersion());

        if (!Files.exists(outputJar)) {
            Files.createDirectories(outputJar.getParent());
            // Create a new JAR file to merge the others into
            Name outputJarName = NameParser.fromMavenGA(model.getGroupId(), model.getArtifactId());
            try (OutputStream jarFile = Files.newOutputStream(outputJar);
                    JarOutputStream mergedJar = new JarOutputStream(jarFile)) {

                // Collect all importmaps to merge at the end
                Map<String, String> importmaps = new HashMap<>();
                // Create new merged pom.xml
                pom.getDependencies().clear();
                Map<String, Dependency> newDependencies = new HashMap<>();
                Map<String, Developer> newDevelopers = new HashMap<>();
                Map<String, License> newLicences = new HashMap<>();
                for (Dependency dependency : dependencies) {
                    Name jarName = NameParser.fromMavenGA(dependency.getGroupId(), dependency.getArtifactId());
                    Path jarPath = mavenRespositoryService.getPath(jarName, dependency.getVersion(), FileType.jar);

                    try (InputStream inputStream = Files.newInputStream(jarPath);
                            JarInputStream inputJar = new JarInputStream(inputStream)) {
                        // Add all entries from the input JAR to the merged JAR
                        JarEntry entry;
                        while ((entry = inputJar.getNextJarEntry()) != null) {

                            String entryname = entry.getName();

                            if (entryname.equals("META-INF/importmap.json")) {
                                // Remember importmap
                                importmaps.putAll(getImportMap(inputJar));
                            } else if (entryname.startsWith("META-INF/maven")) {
                                updatePom(inputJar, newDependencies, newDevelopers, newLicences, entryname,
                                        List.copyOf(mapByGA(dependencies).keySet()));
                            } else if (!entryname.endsWith("LICENSE")) { // Ignore we will add one in root
                                writeEntry(inputJar, mergedJar, entry);
                            }
                        }
                    } catch (IOException | XmlPullParserException e) {
                        throw new RuntimeException(e);
                    }
                }

                // Add importmap (in jar and on disk)
                Aggregator a = new Aggregator(importmaps);
                String aggregatedImportMap = a.aggregateAsJson(false);
                writeEntry(mergedJar, "META-INF/importmap.json", aggregatedImportMap);
                Path importmapPath = getImportMapPath(outputJarName, model.getVersion());
                fileStore.createFile(importmapPath, aggregatedImportMap.getBytes());

                // Add pom (in jar and on disk)
                String jarName = outputJar.toString();
                String pomName = jarName.substring(0, jarName.length() - 3) + "pom";
                Path outputPom = Path.of(pomName);
                pom.setDependencies(List.copyOf(newDependencies.values()));
                pom.setDevelopers(List.copyOf(newDevelopers.values()));
                pom.setLicenses(List.copyOf(newLicences.values()));
                MavenXpp3Writer mxw = new MavenXpp3Writer();

                FileUtil.createDirectories(outputPom);
                try (OutputStream outputStream = Files.newOutputStream(outputPom)) {
                    mxw.write(outputStream, pom);
                    writeEntry(mergedJar, "META-INF/maven/" + pom.getGroupId() + "/" + pom.getArtifactId() + "/pom.xml",
                            outputPom);
                    fileStore.touch(outputJarName, model.getVersion(), outputPom);
                }

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

                Log.info(pom.getGroupId() + ":" + pom.getArtifactId() + ":" + pom.getVersion() + " created");
            }
            // Also create the Sha1
            FileUtil.createSha1(outputJar);

            // Also kick off all other files creation
            fileStore.touch(outputJarName, model.getVersion(), outputJar);
        }
        return pom;
    }

    private void mergeSource(Model pom, List<Dependency> dependencies) throws IOException, XmlPullParserException {
        Path outputJar = fileStore.getLocalFullPath(FileType.source, pom.getGroupId(), pom.getArtifactId(),
                pom.getVersion());

        if (!Files.exists(outputJar)) {
            Files.createDirectories(outputJar.getParent());
            // Create a new JAR file to merge the others into
            try (OutputStream jarFile = Files.newOutputStream(outputJar);
                    JarOutputStream mergedJar = new JarOutputStream(jarFile)) {

                // Create new merged pom.xml
                for (Dependency dependency : dependencies) {
                    Name jarName = NameParser.fromMavenGA(dependency.getGroupId(), dependency.getArtifactId());
                    Path jarPath = mavenRespositoryService.getPath(jarName, dependency.getVersion(), FileType.source);

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
            // Also create the Sha1
            FileUtil.createSha1(outputJar);
            FileUtil.createAsc(outputJar);
            FileUtil.createMd5(outputJar);
        }
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
            try (StringReader sr = new StringReader(jarPom)) {
                Model jarPomModel = mavenXpp3Writer.read(sr);
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
    }

    private Map<String, Dependency> mapByGA(List<Dependency> l) {
        Map<String, Dependency> m = new HashMap<>();
        for (Dependency d : l) {
            m.put(d.getGroupId() + ":" + d.getArtifactId(), d);
        }
        return m;
    }

    private void writeEntry(JarOutputStream mergedJar, String name, String content) throws IOException {
        JarEntry entry = new JarEntry(name);
        mergedJar.putNextEntry(entry);
        mergedJar.write(content.getBytes());
        mergedJar.closeEntry();
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

    private String getEntryContent(JarInputStream inputJar) throws IOException {
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

}
