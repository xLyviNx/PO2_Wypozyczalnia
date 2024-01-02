package src;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Utilities
{
    public static byte[] loadImageAsBytes(String imagePath, boolean isAbsolute) {
        try {

            File f;
            if (isAbsolute)
               f = new File(imagePath);
            else
                f=new File(Main.imagePath + imagePath);
            URL resourceUrl = new URL(f.toURI().toString());
            System.out.println("Absolute URL " + resourceUrl.toString());
            if (resourceUrl != null) {
                try (InputStream stream = resourceUrl.openStream()) {
                    return stream.readAllBytes();
                }
            } else {
                // Handle the case where the resource is not found
                System.out.println("Resource not found: " + resourceUrl);
                return new byte[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., log it or return a default image)
            return new byte[0];
        }
    }
    public static String bytesFormatter(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        double size = bytes / Math.pow(1024, exp);
        return String.format("%.2f %sB", size, unit);
    }
    public static boolean fileExists(URL url) {
        try {
            File file = new File(url.toURI());
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }
}
