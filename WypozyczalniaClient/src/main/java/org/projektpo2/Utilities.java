package org.projektpo2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.*;

/**
 * Klasa narzędziowa zawierająca pomocnicze metody.
 */
public class Utilities {

    /** Obiekt do obsługi logów aplikacji. */
    private static final FileHandler fileHandler;

    static {
        try {
            fileHandler = new FileHandler("client.log");
            fileHandler.setFormatter(new SimpleFormatter());
        } catch (IOException e) {
            System.err.println("Failed to init logs.");
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Metoda zwracająca obiekt Logger dla podanej klasy.
     *
     * @param clazz Klasa, dla której tworzony jest obiekt Logger.
     * @return Obiekt Logger dla danej klasy.
     */
    public static Logger getLogger(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);
        fileHandler.setLevel(Level.ALL);
        return logger;
    }

    /** Obiekt Logger dla klasy Utilities. */
    private static final Logger logger = getLogger(Utilities.class);

    /**
     * Metoda wczytująca obraz w postaci bajtów.
     *
     * @param imagePath Ścieżka do pliku z obrazem.
     * @return Obraz w postaci bajtów.
     */
    public static byte[] loadImageAsBytes(String imagePath) {
        try {
            File f = new File(imagePath);
            URL resourceUrl = new URL(f.toURI().toString());
            logger.log(Level.INFO, "Absolute URL: " + resourceUrl.toString());

            if (resourceUrl != null) {
                try (InputStream stream = resourceUrl.openStream()) {
                    return stream.readAllBytes();
                }
            } else {
                // Obsługa przypadku, gdy zasób nie został odnaleziony
                logger.log(Level.SEVERE, "Resource not found: " + resourceUrl);
                return new byte[0];
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading image", e);
            // Obsługa wyjątku (np. zalogowanie go lub zwrócenie domyślnego obrazu)
            return new byte[0];
        }
    }

    /**
     * Metoda formatująca rozmiar w bajtach do czytelnej postaci.
     *
     * @param bytes Rozmiar w bajtach.
     * @return Sformatowany rozmiar w jednostkach (B, KB, MB, ...).
     */
    public static String bytesFormatter(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        double size = bytes / Math.pow(1024, exp);
        return String.format("%.2f %sB", size, unit);
    }

    /**
     * Metoda sprawdzająca, czy plik istnieje na podanej ścieżce.
     *
     * @param url Ścieżka do pliku.
     * @return True, jeśli plik istnieje; false w przeciwnym przypadku.
     */
    public static boolean fileExists(URL url) {
        try {
            File file = new File(url.toURI());
            return file.exists();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking if file exists", e);
            return false;
        }
    }
}
