package org.mvnpm.file;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.mvnpm.Constants;

public class FileUtil {

    private FileUtil(){
        
    }
    
    public static String sha1(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.SHA1);
            byte[] digest = md.digest(value);
            StringBuilder sb = new StringBuilder(40);
            for (int i = 0; i < digest.length; ++i) {
                sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static void sha1(String outputFile){
        try {
            byte[] content = Files.readAllBytes(Paths.get(outputFile));
            
            String sha1 = FileUtil.sha1(content);
            String localSha1FileName = outputFile + Constants.DOT_SHA1;
            Files.writeString(Paths.get(localSha1FileName), sha1);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public static String md5(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.MD5);
            byte[] digest = md.digest(value);
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static void md5(String outputFile) {
        try {
            byte[] content = Files.readAllBytes(Paths.get(outputFile));
            
            String md5 = FileUtil.md5(content);
            String localMd5FileName = outputFile + Constants.DOT_MD5;
            Files.writeString(Paths.get(localMd5FileName), md5);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public static void asc(String localFileName) {
        try {
            Process process = Runtime.getRuntime().exec(GPG_COMMAND + localFileName);
            ProcessHandle processHandle = process.toHandle();
            
            CompletableFuture<ProcessHandle> onProcessExit = processHandle.onExit();
            onProcessExit.get();
        } catch (InterruptedException | ExecutionException | IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private static final String GPG_COMMAND = "gpg -ab ";
}
