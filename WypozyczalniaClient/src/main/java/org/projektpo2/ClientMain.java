package org.projektpo2;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 * Główna klasa klienta, odpowiedzialna za konfigurację, uruchomienie klienta oraz obsługę braku połączenia.
 */
public class ClientMain {
    private static final Logger logger = Utilities.getLogger(ClientMain.class);

    /** Ścieżka do pliku JAR klienta. */
    public static String pathToJar;
    /** Ścieżka do katalogu zawierającego obrazy. */
    public static String imagePath;
    /** Adres IP serwera. */
    public static String ip;
    /** Numer portu serwera. */
    public static int port;

    /**
     * Konfiguruje klienta ustawiając adres IP i numer portu serwera.
     *
     * @param ip   Adres IP serwera.
     * @param port Numer portu serwera.
     */
    public static void setupClient(String ip, int port) {
        ClientMain.ip = ip;
        ClientMain.port = port;
        setupPaths();
    }

    /**
     * Uruchamia klienta, obsługując wyjątki związane z brakiem połączenia lub błędami klienta.
     */
    public static void runClient() {
        Client cl;
        if (Client.instance != null) {
            cl = Client.instance;
        } else {
            cl = new Client();
        }
        WypozyczalniaOkno wypozyczalniaOkno = WypozyczalniaOkno.instance;
        Thread clTh = getThread(cl);
        try {
            clTh.start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Błąd uruchamiania wątku klienta", ex);
            Client.MessageBox(ex.getLocalizedMessage(), Alert.AlertType.ERROR);
            DisplayNoConnection();
        }
    }
    /**
     * Tworzy wątek klienta.
     *
     * @param cl Obiekt klienta, który będzie obsługiwany przez wątek.
     * @return Nowo utworzony wątek klienta.
     */
    private static Thread getThread(Client cl) {
        Thread clTh = new Thread(() -> {
            try {
                cl.start(ip, port);
            } catch (DisconnectException e) {
                logger.log(Level.SEVERE, "Brak połączenia", e);
                DisplayNoConnection();
                throw e;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Błąd klienta", ex);
                logger.log(Level.SEVERE, ex.getClass().toString(), ex);
                Client.MessageBox(ex.getLocalizedMessage(), Alert.AlertType.ERROR);
                DisplayNoConnection();
            }
        });

        clTh.setName("CLIENT THREAD");
        return clTh;
    }

    /**
     * Wyświetla informację o braku połączenia.
     */
    private static void DisplayNoConnection() {
        if (WypozyczalniaOkno.instance != null)
            WypozyczalniaOkno.instance.NoConnection();
        else
            Platform.exit();
    }

    /**
     * Konfiguruje ścieżki do pliku JAR i katalogu z obrazami.
     */
    private static void setupPaths() {
        pathToJar = ClientMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (pathToJar.endsWith(".jar")) {
            logger.info("Program jest uruchamiany z pliku JAR.");
            setJarImagePath();
        } else {
            logger.info("Program nie jest uruchamiany z pliku JAR.");
        }
    }

    /**
     * Ustawia ścieżkę do katalogu z obrazami na podstawie lokalizacji pliku JAR.
     */
    private static void setJarImagePath() {
        File jarFile = new File(pathToJar);
        imagePath = jarFile.getParent() + File.separator + "img/";
    }

    /**
     * Metoda główna, uruchamiająca aplikację.
     *
     * @param args Argumenty wiersza poleceń.
     */
    public static void main(String[] args) {
        WypozyczalniaOkno.main(args);
    }
}
