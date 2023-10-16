package io.mvnpm.file.type;

import static io.mvnpm.Constants.CLOSE_ROUND;
import static io.mvnpm.Constants.COMMA;
import static io.mvnpm.Constants.OPEN_BLOCK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Bugs;
import io.mvnpm.npm.model.Maintainer;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.Repository;
import io.mvnpm.version.VersionConverter;

/**
 * Creates a pom.xml from the NPM Package
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class PomClient {

    @Inject
    FileStore fileCreator;

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    private final MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();

    public byte[] createPom(io.mvnpm.npm.model.Package p, Path localFilePath) {
        byte[] contents = writePomToBytes(p);
        return fileCreator.createFile(p, localFilePath, contents);
    }

    private byte[] writePomToBytes(io.mvnpm.npm.model.Package p) {

        List<Dependency> deps = toDependencies(p.dependencies());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Model model = new Model();

            model.setModelVersion(MODEL_VERSION);
            model.setGroupId(p.name().mvnGroupId());
            model.setArtifactId(p.name().mvnArtifactId());
            model.setVersion(p.version());
            model.setPackaging(JAR);
            model.setName(p.name().displayName());
            if (p.description() == null || p.description().isEmpty()) {
                model.setDescription(p.name().displayName());
            } else {
                model.setDescription(p.description());
            }

            model.setLicenses(toLicenses(p.license()));
            model.setScm(toScm(p.repository()));
            model.setUrl(toUrl(model, p.homepage()));
            model.setOrganization(toOrganization(p));

            model.setIssueManagement(toIssueManagement(p.bugs()));
            model.setDevelopers(toDevelopers(p.maintainers()));
            if (!deps.isEmpty()) {
                Properties properties = new Properties();

                for (Dependency dep : deps) {
                    String version = dep.getVersion();
                    String propertyKey = dep.getGroupId() + Constants.HYPHEN + dep.getArtifactId() + Constants.DOT
                            + Constants.VERSION;
                    properties.put(propertyKey, version);
                    dep.setVersion(Constants.DOLLAR + Constants.OPEN_CURLY + propertyKey + Constants.CLOSE_CURLY);
                }

                model.setProperties(properties);
                model.setDependencies(deps);
            }
            mavenXpp3Writer.write(baos, model);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toUrl(Model model, URL homepage) {
        if (homepage != null) {
            return homepage.toString();
        } else if (model.getScm() != null && model.getScm().getUrl() != null) {
            return model.getScm().getUrl();
        } else if (model.getScm() != null && model.getScm().getConnection() != null) {
            return model.getScm().getConnection();
        } else if (model.getScm() != null && model.getScm().getDeveloperConnection() != null) {
            return model.getScm().getDeveloperConnection();
        } else {
            return "http://mvnpm.org"; // If all else fail, set our URL, as an empty one fails the oss sonatype validation
        }
    }

    private List<License> toLicenses(io.mvnpm.npm.model.License license) {
        License l = new License();

        if (license != null) {
            if (license.type() != null) {
                l.setName(license.type());
            }
            if (license.url() != null) {
                l.setUrl(license.url().toString());
            }
        }

        if (l.getName() == null && l.getUrl() == null) {
            l.setName("none");
        }
        return List.of(l);
    }

    private Organization toOrganization(io.mvnpm.npm.model.Package p) {
        Organization o = new Organization();
        if (p.author() != null) {
            o.setName(p.author().name());
        } else {
            o.setName(p.name().displayName());
        }
        if (p.homepage() != null) {
            o.setUrl(p.homepage().toString());
        }
        return o;
    }

    private IssueManagement toIssueManagement(Bugs bugs) {
        if (bugs != null && bugs.url() != null) {
            IssueManagement i = new IssueManagement();
            i.setUrl(bugs.url().toString());
            return i;
        }
        return null;
    }

    private Scm toScm(Repository repository) {
        if (repository != null && repository.url() != null && !repository.url().isEmpty()) {
            String u = repository.url();
            if (u.startsWith(GIT_PLUS)) {
                u = u.substring(GIT_PLUS.length());
            }
            String conn = u;
            String repo = u;
            if (repo.endsWith(DOT_GIT)) {
                repo = repo.substring(0, repo.length() - DOT_GIT.length());
            }
            if (!conn.endsWith(DOT_GIT)) {
                conn = conn + DOT_GIT;
            }
            Scm s = new Scm();
            s.setUrl(repo);
            s.setConnection(conn);
            s.setDeveloperConnection(conn);
            return s;
        } else {
            Scm s = new Scm();
            s.setUrl("https://github.com/mvnpm/mvnpm.git");
            s.setConnection("https://github.com/mvnpm/mvnpm.git");
            s.setDeveloperConnection("https://github.com/mvnpm/mvnpm.git");
            return s;
        }

    }

    private List<Developer> toDevelopers(List<Maintainer> maintainers) {
        List<Developer> ds = new ArrayList<>();
        if (maintainers != null && !maintainers.isEmpty()) {
            for (Maintainer m : maintainers) {
                Developer d = new Developer();
                d.setEmail(m.email());
                d.setName(m.name());
                ds.add(d);
            }
        }
        if (ds.isEmpty()) {
            Developer d = new Developer();
            d.setName("unknown");
            ds.add(d);
        }
        return ds;
    }

    private List<Dependency> toDependencies(Map<Name, String> dependencies) {
        List<Dependency> deps = new ArrayList<>();
        if (dependencies != null && !dependencies.isEmpty()) {
            for (Map.Entry<Name, String> e : dependencies.entrySet()) {
                Name name = e.getKey();
                String version = e.getValue();
                deps.add(toDependency(name, version));
            }
            return deps;
        } else {
            return List.of();
        }
    }

    private Dependency toDependency(Name name, String version) {
        Dependency d = new Dependency();
        d.setGroupId(name.mvnGroupId());
        d.setArtifactId(name.mvnArtifactId());
        d.setVersion(toVersion(name, version));
        return d;
    }

    private String toVersion(Name name, String version) {
        String trimVersion = VersionConverter.convert(version).trim().replaceAll("\\s+", "");

        // This is an open ended range. Let's get the latest for a bottom boundary
        if (trimVersion.equals(OPEN_BLOCK + COMMA + CLOSE_ROUND)) {
            Project project = npmRegistryFacade.getProject(name.npmFullName());
            return OPEN_BLOCK + project.distTags().latest() + COMMA + CLOSE_ROUND;
        }
        // TODO: Make other ranges more effient too ?
        return trimVersion;
    }

    private static final String JAR = "jar";

    private static final String MODEL_VERSION = "4.0.0";
    private static final String GIT_PLUS = "git+";
    private static final String DOT_GIT = ".git";

}
