package org.mavenpm.file;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import java.io.File;
import java.net.URL;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Downloads or stream the tar files from npm
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * TODO: Error handling (when version / package does not exist)
 * TODO: Add metrics / analytics / eventing ?
 */
@ApplicationScoped
public class TarFileClient {

    @Inject
    Vertx vertx;
    
    private final OpenOptions readOnlyOptions = (new OpenOptions()).setCreate(false).setWrite(false);
    private final String cacheDir = System.getProperty(USER_HOME);
    
    public Uni<AsyncFile> streamSha1(org.mavenpm.npm.model.Package p) {
        String localFileName = getLocalShaFullPath(p);
        
        Uni<Boolean> checkIfLocal = vertx.fileSystem().exists(localFileName);
        return checkIfLocal.onItem()
                .transformToUni((local) -> { 
                    return isLocalSha1(p, localFileName, local);
                });
    }
    
    public Uni<AsyncFile> streamTgz(org.mavenpm.npm.model.Package p) {
        String localFileName = getLocalTgzFullPath(p);
        
        Uni<Boolean> checkIfLocal = vertx.fileSystem().exists(localFileName);
        return checkIfLocal.onItem()
                .transformToUni((local) -> { 
                    return isLocalTgz(p, localFileName, local);
                });
    }
    
    private Uni<AsyncFile> isLocalTgz(org.mavenpm.npm.model.Package p, String localFileName, Boolean local){
        if(local){
            return streamLocal(localFileName);
        }else{
            return fetchRemote(p, localFileName);
        }
    }
    
    private Uni<AsyncFile> isLocalSha1(org.mavenpm.npm.model.Package p, String localFileName, Boolean local){
        if(local){
            return streamLocal(localFileName);
        }else{
            String localTgzFileName = getLocalTgzFullPath(p);
            Uni<AsyncFile> fetchTgzRemote = fetchRemote(p, localTgzFileName);
            return fetchTgzRemote.onItem().transformToUni((downloaded) -> {
                return streamLocal(localFileName);
            });
        }
    }
    
    private Uni<AsyncFile> streamLocal(String localFileName){
        Log.info("...available locally [" + localFileName + "]");
        return vertx.fileSystem().open(localFileName, readOnlyOptions);
    }
    
    private Uni<AsyncFile> fetchRemote(org.mavenpm.npm.model.Package p, String localFileName){
        URL tarball = p.dist().tarball();
        
        Log.info("... NOT available locally, downloading " + tarball.toString());
        
        RequestOptions requestOptions = getRequestOptions(tarball);
        Uni<HttpClientRequest> u = vertx.createHttpClient().request(requestOptions);
        return u.onItem().transformToUni((req) -> {
            return request(p, localFileName, req);
        });
    }
    
    private Uni<AsyncFile> request(org.mavenpm.npm.model.Package p, String localFileName, HttpClientRequest req){
        return req.connect().onItem().transformToUni((res) -> {
            
            int statusCode = res.statusCode();
            if(statusCode == 200){
                return response(p, localFileName, res);
            }else {
                throw new RuntimeException("Error download tar from NPM " + req.getURI() + " [" + statusCode + "] - " + res.statusMessage());
            }
        });
    }
    
    private Uni<AsyncFile> response(org.mavenpm.npm.model.Package p, String localFileName, HttpClientResponse res){
        return res.body().onItem().transformToUni((body)  -> {
            Uni<Void> createdDir = vertx.fileSystem().mkdirs(getLocalDirectory(p));
            return createdDir.onItem().transformToUni((createdDirs) -> {
                Uni<Void> emptyFile = vertx.fileSystem().createFile(localFileName);
                return emptyFile.onItem().transformToUni((createdFile) -> {
                    Uni<Void> download = vertx.fileSystem().writeFile(localFileName, body);
                    return download.onItem().transformToUni((doneDownload) -> {
                        String sha1 = Sha1Util.sha1(body.getBytes());
                        String localSha1FileName = localFileName + DOT + SHA1;
                        Uni<Void> emptySha1File = vertx.fileSystem().createFile(localSha1FileName);
                        return emptySha1File.onItem().transformToUni((createdSha) -> {
                            Uni<Void> writtenSha = vertx.fileSystem().writeFile(localSha1FileName, Buffer.buffer(sha1));
                            return writtenSha.onItem().transformToUni((doneSha) -> {
                                return streamLocal(localFileName);
                            });
                        });
                    });
                });
            });
        });
    }
    
    private RequestOptions getRequestOptions(URL u){
        RequestOptions requestOptions = new RequestOptions();
        if(u.getPort()>0){
            requestOptions.setPort(u.getPort());
        } else if(HTTPS.equalsIgnoreCase(u.getProtocol())){
            requestOptions.setPort(443);
            requestOptions.setSsl(Boolean.TRUE);
        } else {
            requestOptions.setPort(80);
            requestOptions.setSsl(Boolean.FALSE);
        }
        
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setHost(u.getHost());
        requestOptions.setURI(u.getPath());
        return requestOptions;
    }
    
    
    private String getLocalDirectory(org.mavenpm.npm.model.Package p){
        return cacheDir + File.separator + 
                DOT + Q2 + File.separator +
                REPOSITORY + File.separator + 
                ORG + File.separator + 
                MAVENPM + File.separator + 
                p.name() + File.separator + 
                p.version();
    }
    
    private String getLocalShaFullPath(org.mavenpm.npm.model.Package p){
        return getLocalTgzFullPath(p) + DOT + SHA1;
    }
    
    private String getLocalTgzFullPath(org.mavenpm.npm.model.Package p){
        return getLocalDirectory(p) + File.separator + 
                getLocalTgzFileName(p);
    }
    
    public String getLocalTgzFileName(org.mavenpm.npm.model.Package p){
        return p.name() + DASH + p.version() + DOT + TGZ;
    }
    
    public String getLocalSha1FileName(org.mavenpm.npm.model.Package p){
        return getLocalTgzFileName(p) + DOT + SHA1;
    }
    
    private static final String REPOSITORY = "repository";
    private static final String DASH = "-";
    private static final String DOT = ".";
    private static final String Q2 = "q2";
    private static final String ORG = "org";
    private static final String MAVENPM = "mavenpm";
    private static final String TGZ = "tgz";
    private static final String HTTPS = "https";
    private static final String USER_HOME = "user.home";
    private static final String SHA1 = "sha1";
}
