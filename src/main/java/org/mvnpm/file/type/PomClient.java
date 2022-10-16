package org.mvnpm.file.type;

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
import org.mvnpm.file.FileStore;
import org.mvnpm.npm.model.Author;
import org.mvnpm.npm.model.Bugs;
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
        model.setGroupId(GROUP_ID);
        model.setArtifactId(p.name());
        model.setVersion(p.version());
        model.setPackaging(JAR);
        model.setName(toName(p.name()));
        model.setDescription(p.description());
        model.setLicenses(List.of(toLicense(p.license())));
        model.setUrl(p.homepage().toString());
        model.setOrganization(toOrganization(p));
        model.setScm(toScm(p.repository()));
        model.setIssueManagement(toIssueManagement(p.bugs()));
        model.setDevelopers(toDevelopers(p.maintainers()));
        model.setDependencies(toDependencies(p.dependencies()));
        mavenXpp3Writer.write(entityStream, model);
    }
    
    private License toLicense(String license){
        License l = new License();
        l.setName(license);
        return l;
    }
    
    private Organization toOrganization(org.mvnpm.npm.model.Package p){
        Organization o = new Organization();
        if(p.author()!=null){
            o.setName(p.author().name());
        }else{
            o.setName(p.name());
        }
        if(p.homepage()!=null){
            o.setUrl(p.homepage().toString());
        }
        return o;
    }
    
    private IssueManagement toIssueManagement(Bugs bugs){
        IssueManagement i = new IssueManagement();
        i.setUrl(bugs.url().toString());
        return i;
    }
    
    // TODO: Clean this a bit
    private Scm toScm(Repository repository){
        Scm s = new Scm();
        s.setUrl(repository.url());
        s.setConnection(repository.url());
        s.setDeveloperConnection(repository.url());
        return s;
    }
    
    private String toName(String name){
        if(name.contains(AT)){
            name = name.replace(AT, EMPTY);
        }
        if(name.contains(SLASH)){
            name = name.replace(SLASH, SPACE);
        }
        
        return name;
    }
    
    private List<Developer> toDevelopers(List<Maintainer> maintainers){
        List<Developer> ds = new ArrayList<>();
        for(Maintainer m:maintainers){
            Developer d = new Developer();
            d.setEmail(m.email());
            d.setName(m.name());
            ds.add(d);
        }
        return ds;
    }
    
    private List<Dependency> toDependencies(Map<String, String> dependencies){
        if(dependencies!=null && !dependencies.isEmpty()){
            List<Dependency> ds = new ArrayList<>();
            for(Map.Entry<String,String> e:dependencies.entrySet()){
                String artifactId = e.getKey();
                String version = e.getValue();
                Dependency d = new Dependency();
                d.setGroupId(GROUP_ID);
                d.setArtifactId(artifactId);
                d.setVersion(toVersion(version));
                d.setScope(RUNTIME);
                ds.add(d);
            }
            return ds;
        }
        return Collections.EMPTY_LIST;
    }
    
    private String toVersion(String version){
        if(version.startsWith(CARET)){
            version = version.replace(CARET, EMPTY);
            
        }
        return version.trim();
    }
    
    private static final String JAR = "jar";
    private static final String GROUP_ID = "org.mvnpm";
    private static final String RUNTIME = "runtime";
    private static final String AT = "@";
    private static final String EMPTY = "";
    private static final String SLASH = "/";
    private static final String SPACE = " ";
    private static final String CARET = "^";
    
}
