package ruc.irm.wikit.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZip util for compress and de-compress string
 *
 * @author Tian Xia
 * @date May 02, 2015 9:19 AM
 */
public class GZipUtils {
    public static byte[] gzip(String text, String encoding) {
        if (text == null) {
            return null;
        } else if (text.length()==0){
            return new byte[0];
        }

        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(out);) {
            gzip.write(text.getBytes("UTF-8"));
            gzip.close();
            out.close();
            byte[] compressedBytes = out.toByteArray();
            return compressedBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String unzip(byte[] bytes, String encoding) {
        if (bytes == null) {
            return null;
        } else if (bytes.length == 0) {
            return "";
        }
        try (
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                GZIPInputStream gin = new GZIPInputStream(input);
                ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            byte[] buffer = new byte[1024 * 8];
            int n;
            while ((n = gin.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            out.flush();
            return out.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

    }
}
