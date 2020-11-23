package cn.coderblue.mongodb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author coderblue
 */
public class MD5 {
    /**
     * 获取输入流的md5小写值
     * @param in 输入流
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getMd5(InputStream in) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] bytes = new byte[1024];
        int byteCount = 0;
        while ((byteCount = in.read(bytes)) !=-1) {
            messageDigest.update(bytes, 0, byteCount);
        }
        byte[] digest = messageDigest.digest();
        // byte -128 ---- 127
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            int a = b & 0xff;
            String hex = Integer.toHexString(a);
            if (hex.length() == 1) {
                hex = 0 + hex;
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase();
    }
}
