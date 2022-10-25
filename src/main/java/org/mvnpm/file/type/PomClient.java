package org.mvnpm.file.type;

import com.github.villadora.semver.SemVer;
import com.github.villadora.semver.Version;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.mvnpm.Constants;
import org.mvnpm.file.FileStore;
import org.mvnpm.file.metadata.MetadataClient;
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
    
    @Inject 
    MetadataClient metadataClient;
    
    private final MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
    
    public Uni<AsyncFile> createPom(org.mvnpm.npm.model.Package p, String localFileName) {     
        Uni<byte[]> contents = writePomToStream(p);
        return contents.onItem().transformToUni((c) -> {
            return fileCreator.createFile(p, localFileName, c);
        });
    }
    
    private Uni<byte[]> writePomToStream(org.mvnpm.npm.model.Package p) {
        
        Uni<List<Dependency>> toDependencies = toDependencies(p.dependencies());
        return toDependencies.onItem().transform((deps) -> {
            try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
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
                if(!deps.isEmpty()){
                    model.setDependencies(deps);
                }
                mavenXpp3Writer.write(baos, model);
                return baos.toByteArray();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
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
    
    private Uni<List<Dependency>> toDependencies(Map<Name, String> dependencies){
        List<Uni<Dependency>> deps = new ArrayList<>();
        if(dependencies!=null && !dependencies.isEmpty()){
            for(Map.Entry<Name,String> e:dependencies.entrySet()){
                Name name = e.getKey();
                String version = e.getValue();
                deps.add(toDependency(name, version));
            }
        }
        
        if(!deps.isEmpty()){
            Uni<List<Dependency>> all = Uni.join().all(deps).andCollectFailures();
        
            return all.onItem().transform((a)->{
                List<Dependency> ds = new ArrayList<>();
                ds.addAll(a);
                return ds;
            });
        }else {
            return Uni.createFrom().item(List.of());
        }
        
    }
    
    private Uni<Dependency> toDependency(Name name, String version){
        Uni<String> convertedVersion = toVersion(name, version);
        return convertedVersion.onItem().transform((cv)-> {
            Dependency d = new Dependency();
            d.setGroupId(name.mvnGroupId());
            d.setArtifactId(name.mvnArtifactId());
            d.setVersion(cv);
            return d;
        });
    }
    
    // TODO: This needs more work
    // see https://docs.npmjs.com/cli/v6/using-npm/semver#ranges
    
    private Uni<String> toVersion(Name name, String version){
        
        if(SemVer.valid(version)){
            Version v = SemVer.version(version);
            return Uni.createFrom().item(v.toString());
        } else if(SemVer.rangeValid(version)){
            String range = SemVer.range(version).toString();
            String[] maxMin = range.split(Constants.SPACE);
            if(maxMin.length!=2){
                Log.warn("Could not parse range " + range + " for " + name.npmFullName());
                return Uni.createFrom().item(exactVersion(version));
            }
            String maxPart = maxMin[0];
            String minPart = maxMin[1];
            String minBracket = Constants.EMPTY;
            String maxBracket = Constants.EMPTY;
            String min = Constants.ZERO_ZERO_ONE;
            String max = Constants.ZERO_ZERO_ONE;
            
            if(minPart.startsWith(GREATER_THAN + EQUALS)){
                minBracket = OPEN_BLOCK_BRACKET;
                min = minPart.substring(2);
            }else if(minPart.startsWith(GREATER_THAN)){
                minBracket = OPEN_ROUND_BRACKET;
                min = minPart.substring(1);
            }
            if(maxPart.startsWith(LESS_THAN + EQUALS)){
                maxBracket = CLOSE_BLOCK_BRACKET;
                max = maxPart.substring(2);
            }else if(maxPart.startsWith(LESS_THAN)){
                maxBracket = CLOSE_ROUND_BRACKET;
                max = maxPart.substring(1);
            }
            
            // Try and get the latest for the min
            return getBestVersionRange(name, min, max, minBracket, maxBracket);
            
        } else {
            Log.warn("Could not parse version " + version + " for " + name.npmFullName());
            return Uni.createFrom().item(exactVersion(version));
        }
    }
    
    private Uni<String> getBestVersionRange(Name name, String min, String max, String minBracket, String maxBracket){
        
        Uni<Versioning> versioning = metadataClient.getVersioning(name);
        return versioning.onItem().transform((v) -> {
            final String latest = v.getLatest();
            // If the latest Version is beteen the min and max, ver can use that.
            if(SemVer.lt(latest, max)){
                Version maxVersion = SemVer.version(max);
                return minBracket + latest + Constants.COMMA + maxVersion.toString() + maxBracket;
            }
            // TODO: As a second attempt try and find it in all the versions
            Version maxVersion = SemVer.version(max);
            return minBracket + min + Constants.COMMA + maxVersion.toString() + maxBracket;
        });
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
