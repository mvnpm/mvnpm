package org.mavenpm.file.type;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import java.net.URL;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.mavenpm.file.FileStore;

/**
 * Downloads or stream the tar files from npm
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * TODO: Error handling (when version / package does not exist)
 * TODO: Add metrics / analytics / eventing ?
 */
@ApplicationScoped
public class TgzClient {

    @Inject
    Vertx vertx;
    
    @Inject 
    FileStore fileCreator;
    
    public Uni<AsyncFile> fetchRemote(org.mavenpm.npm.model.Package p, String localFileName){
        URL tarball = p.dist().tarball();
        
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
            return fileCreator.createFile(p, localFileName, body.getBytes());
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
    
    private static final String HTTPS = "https";
}
