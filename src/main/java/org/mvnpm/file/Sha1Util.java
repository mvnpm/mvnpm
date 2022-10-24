package org.mvnpm.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.mvnpm.Constants;

public class Sha1Util {

    private Sha1Util(){
        
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
}
