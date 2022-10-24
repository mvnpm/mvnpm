package org.mvnpm.file.type;

import com.github.villadora.semver.SemVer;
import com.github.villadora.semver.Version;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.Constants;
import org.mvnpm.file.FileStore;
import org.mvnpm.npm.model.Bugs;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.Maintainer;
import org.mvnpm.npm.model.Repository;

/**
 * Creates a pom.xml from the NPM Package
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class PomClient {
    
    @Inject 
    FileStore fileCreator;
    
    @ConfigProperty(name = "mvnpm.importmap-version", defaultValue = "1.0.1")
    String importMapVersion;
    
    private final MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
    
    public Uni<AsyncFile> createPom(org.mvnpm.npm.model.Package p, String localFileName) {
                
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            writePomToStream(p, baos);
            byte[] contents = baos.toByteArray();
            return fileCreator.createFile(p, localFileName, contents);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void writePomToStream(org.mvnpm.npm.model.Package p, OutputStream entityStream) throws IOException {
        Model model = new Model();
        model.setModelVersion(MODEL_VERSION);
        model.setGroupId(p.name().mvnGroupId());
        model.setArtifactId(p.name().mvnArtifactId());
        model.setVersion(p.version());
        model.setPackaging(JAR);
        model.setName(p.name().displayName());
        model.setDescription(p.description());
        model.setLicenses(toLicenses(p.license()));
        if(p.homepage()!=null)model.setUrl(p.homepage().toString());
        model.setOrganization(toOrganization(p));
        model.setScm(toScm(p.repository()));
        model.setIssueManagement(toIssueManagement(p.bugs()));
        model.setDevelopers(toDevelopers(p.maintainers()));
        model.setDependencies(toDependencies(p.dependencies()));
        mavenXpp3Writer.write(entityStream, model);
    }
    
    private List<License> toLicenses(String license){
        if(license!=null && !license.isEmpty()){
            License l = new License();
            l.setName(license);
            return List.of(l);
        }
        return Collections.EMPTY_LIST;
    }
    
    private Organization toOrganization(org.mvnpm.npm.model.Package p){
        Organization o = new Organization();
        if(p.author()!=null){
            o.setName(p.author().name());
        }else{
            o.setName(p.name().displayName());
        }
        if(p.homepage()!=null){
            o.setUrl(p.homepage().toString());
        }
        return o;
    }
    
    private IssueManagement toIssueManagement(Bugs bugs){
        if(bugs!=null && bugs.url()!=null){
            IssueManagement i = new IssueManagement();
            i.setUrl(bugs.url().toString());
            return i;
        }
        return null;
    }
    
    private Scm toScm(Repository repository){
        if(repository!=null && repository.url()!=null && !repository.url().isEmpty()){
            String u = repository.url();
            if(u.startsWith(GIT_PLUS)){
                u = u.substring(GIT_PLUS.length());
            }
            String conn = u;
            String repo = u;
            if(repo.endsWith(DOT_GIT)){
                repo = repo.substring(0, repo.length() - DOT_GIT.length());
            }
            if(!conn.endsWith(DOT_GIT)){
                conn = conn + DOT_GIT;
            }
            Scm s = new Scm();
            s.setUrl(repo);
            s.setConnection(conn);
            s.setDeveloperConnection(conn);
            return s;
        }
        return null;
    }
    
    private List<Developer> toDevelopers(List<Maintainer> maintainers){
        if(maintainers!=null && !maintainers.isEmpty()){
            List<Developer> ds = new ArrayList<>();
            for(Maintainer m:maintainers){
                Developer d = new Developer();
                d.setEmail(m.email());
                d.setName(m.name());
                ds.add(d);
            }
            return ds;
        }
        return Collections.EMPTY_LIST;
    }
    
    private List<Dependency> toDependencies(Map<Name, String> dependencies){
        List<Dependency> ds = new ArrayList<>();
        if(dependencies!=null && !dependencies.isEmpty()){
            for(Map.Entry<Name,String> e:dependencies.entrySet()){
                Name name = e.getKey();
                String version = e.getValue();
                Dependency d = new Dependency();
                d.setGroupId(name.mvnGroupId());
                d.setArtifactId(name.mvnArtifactId());
                d.setVersion(toVersion(name, version));
                d.setScope(RUNTIME);
                ds.add(d);
            }
        }
        
        // Also add mvnpm importmap dependency
        Dependency d = new Dependency();
        d.setGroupId(Constants.ORG_DOT_MVNPM);
        d.setArtifactId(Constants.IMPORTMAP);
        d.setVersion(importMapVersion);
        
        return ds;
    }
    
    // TODO: This needs more work
    // see https://docs.npmjs.com/cli/v6/using-npm/semver#ranges
    
    private String toVersion(Name name, String version){
        
        if(SemVer.valid(version)){
            Version v = SemVer.version(version);
            return v.toString();
        } else if(SemVer.rangeValid(version)){
            String range = SemVer.range(version).toString();
            String[] maxMin = range.split(Constants.SPACE);
            if(maxMin.length!=2){
                Log.warn("Could not parse range " + range + " for " + name.npmFullName());
                return exactVersion(version);
            }
            String max = maxMin[0];
            String min = maxMin[1];
            
            if(min.startsWith(GREATER_THAN + EQUALS)){
                min = OPEN_BLOCK_BRACKET + min.substring(2);
            }else if(min.startsWith(GREATER_THAN)){
                min = OPEN_ROUND_BRACKET + min.substring(1);
            }
            if(max.startsWith(LESS_THAN + EQUALS)){
                max = max.substring(2) + CLOSE_BLOCK_BRACKET;
            }else if(max.startsWith(LESS_THAN)){
                max = max.substring(1) + CLOSE_ROUND_BRACKET;
            }
            
            return min + Constants.COMMA + max;
        } else {
            Log.warn("Could not parse version " + version + " for " + name.npmFullName());
            return exactVersion(version);
        }
    }
    
    
    /**
     * Removes all range indicators
     */
    private String exactVersion(String v){
        if(v.startsWith(LESS_THAN) 
                || v.startsWith(GREATER_THAN) 
                || v.startsWith(EQUALS)
                || v.startsWith(CARET)
                || v.startsWith(TILDE)){
            v = v.substring(1);
            return exactVersion(v);
        }
        return v;
    }
    
    private static final String JAR = "jar";
    
    private static final String RUNTIME = "runtime";
    
    private static final String MODEL_VERSION = "4.0.0";
    private static final String GIT_PLUS = "git+";
    private static final String DOT_GIT = ".git";
    
    private static final String LESS_THAN = "<";
    private static final String GREATER_THAN = ">";
    private static final String EQUALS = "=";
    private static final String CARET = "^";
    private static final String TILDE = "~";
    
    private static final String OPEN_BLOCK_BRACKET = "[";
    private static final String CLOSE_BLOCK_BRACKET = "]";
    
    private static final String OPEN_ROUND_BRACKET = "(";
    private static final String CLOSE_ROUND_BRACKET = ")";
}
