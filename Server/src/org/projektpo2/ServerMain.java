package org.projektpo2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa główna uruchamiająca serwer.
 */
public class ServerMain {
    private static final Logger logger = Utilities.getLogger(ServerMain.class);

    /** Ścieżka do pliku JAR lub klasy. */
    public static String pathToJar;

    /** Ścieżka do katalogu z obrazami. */
    public static String imagePath;

    /** Flaga określająca, czy program jest uruchamiany z pliku JAR. */
    public static boolean isJar;

    /** Port, na którym działa serwer. */
    public static int port;

    /**
     * Metoda główna uruchamiająca serwer.
     *
     * @param args Argumenty wywołania programu.
     */
    public static void main(String[] args) {
        initializePaths();
        if (args.length == 6) {
            initializeImageDirectory();
            initializeServer(args);
        } else {
            logger.log(Level.SEVERE, "Nieprawidłowa liczba argumentów. Oczekiwano 6 argumentów.");
        }
    }

    /**
     * Inicjalizuje ścieżki programu.
     */
    private static void initializePaths() {
        pathToJar = ServerMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (pathToJar.endsWith(".jar")) {
            logger.info("Program jest uruchamiany z pliku JAR.");
            File jarFile = new File(pathToJar);
            imagePath = jarFile.getParent() + File.separator + "img/";
        } else {
            logger.info("Program nie jest uruchamiany z pliku JAR.");
            ClassLoader classLoader = Server.class.getClassLoader();
            URL resourceUrl = classLoader.getResource("");
            if (resourceUrl != null) {
                String classpath = new File(resourceUrl.getFile()).getPath();
                imagePath = classpath + File.separator + "img/";
            } else {
                logger.log(Level.SEVERE, "Nie można utworzyć ścieżki img.");
                System.exit(1);
            }
        }
    }

    /**
     * Inicjalizuje katalog z obrazami.
     */
    private static void initializeImageDirectory() {
        logger.info("Images path: " + imagePath);
        Path folder = Paths.get(imagePath);
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
                logger.info("Katalog img został utworzony pomyślnie.");
            } catch (IOException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Błąd podczas tworzenia katalogu img.", e);
                System.exit(1);
            }
        }
    }

    /**
     * Inicjalizuje serwer.
     *
     * @param args Argumenty wywołania programu.
     */
    private static void initializeServer(String[] args) {
        logger.info("Server is starting...");
        port = Integer.parseInt(args[0].trim());
        DatabaseHandler.setCredentials(args[1], args[2], args[3], args[4], args[5]);
        Server sv = new Server();
        try {
            sv.start(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
