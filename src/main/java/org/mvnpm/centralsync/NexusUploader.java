package org.mvnpm.centralsync;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Uploads files to Nexus
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class NexusUploader {

    @ConfigProperty(name = "mvnpm.sonatype.url", defaultValue = "https://s01.oss.sonatype.org/service/local/staging/bundle_upload")
    String url;
    @ConfigProperty(name = "mvnpm.sonatype.username", defaultValue = "nothing")
    private String username;
    @ConfigProperty(name = "mvnpm.sonatype.password", defaultValue = "nothing")
    private String password;
    @ConfigProperty(name = "mvnpm.sonatype.mockupload")
    private boolean mock;
    
    public void upload(String fileName){
        Log.debug("====== mvnpm: Nexus Uploader ======");
        Log.debug("\tUploading " + fileName + "...");
        
        if(mock){
            mockUpload(fileName);
        }else{
            realUpload(fileName);
        }
    }
    
    private void mockUpload(String fileName){
        Log.info("Mock Uploaded " + fileName + " successful");
    }
    
    private void realUpload(String fileName){
        File file = new File(fileName);
        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", "Basic " + getBase64AuthString(username, password));

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

        HttpEntity multipart = builder.build();
        post.setEntity(multipart);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = httpClient.execute(post);
            
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == 201){
                Log.info("Uploaded " + fileName + " successful");
            }else{
                throw new RuntimeException("Upload failed for " + fileName + " " + statusCode + ": " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private String getBase64AuthString(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = org.apache.commons.codec.binary.Base64.encodeBase64(auth.getBytes());
        return new String(encodedAuth);
    }   
}