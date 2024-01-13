package org.projektpo2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerMain {
    public static String pathToJar;
    public static String imagePath;
    public static boolean isJar;
    public static String ip;
    public static int port;

    public static void main(String[] args) {
        initializePaths();
        if (args.length == 7) {
            initializeImageDirectory();
            initializeServer(args);
        } else {
            System.err.println("Nieprawidłowa liczba argumentów. Oczekiwano 7 argumentów.");
        }
    }

    private static void initializePaths() {
        pathToJar = ServerMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (pathToJar.endsWith(".jar")) {
            System.out.println("Program jest uruchamiany z pliku JAR.");
            File jarFile = new File(pathToJar);
            imagePath = jarFile.getParent() + File.separator + "img/";
        } else {
            System.out.println("Program nie jest uruchamiany z pliku JAR.");
            ClassLoader classLoader = Server.class.getClassLoader();
            URL resourceUrl = classLoader.getResource("");
            if (resourceUrl != null) {
                String classpath = new File(resourceUrl.getFile()).getPath();
                imagePath = classpath + File.separator + "img/";
            } else {
                System.out.println("Nie można utworzyć ścieżki img.");
                System.exit(1);
            }
        }
    }

    private static void initializeImageDirectory() {
        System.out.println("Images path: " + imagePath);
        Path folder = Paths.get(imagePath);
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
                System.out.println("Katalog img został utworzony pomyślnie.");
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Błąd podczas tworzenia katalogu img.");
                System.exit(1);
            }
        }
    }

    private static void initializeServer(String[] args) {
        System.out.println("Server is starting...");
        ip = args[0];
        port = Integer.parseInt(args[1].trim());
        DatabaseHandler.setCredentials(args[2], args[3], args[4], args[5], args[6]);
        Server sv = new Server();
        try {
            sv.start(ip, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
